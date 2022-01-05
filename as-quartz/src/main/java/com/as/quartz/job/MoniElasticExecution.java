package com.as.quartz.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.domain.MoniElasticLog;
import com.as.quartz.service.IMoniApiService;
import com.as.quartz.service.IMoniElasticLogService;
import com.as.quartz.service.IMoniElasticService;
import com.as.quartz.util.AbstractQuartzJob;
import com.as.quartz.util.OkHttpUtils;
import com.as.quartz.util.ScheduleUtils;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * SQL检测任务执行类（禁止并发执行）
 *
 * @author kolin
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MoniElasticExecution extends AbstractQuartzJob {
    private static final Logger log = LoggerFactory.getLogger(MoniElasticExecution.class);

    /**
     * 创建任务名时使用的前缀，如ELASTIC-JOB-1
     */
    private static final String JOB_CODE = "ELASTIC-JOB";

    private static final String JOB_DETAIL_URL = "/monitor/elasticJob/detail/";
    private static final String LOG_DETAIL_URL = "/monitor/elasticJobLog/detail/";

    private final MoniElasticLog moniElasticLog = new MoniElasticLog();

    private MoniElastic moniElastic = new MoniElastic();

    private Boolean isWebhook;

    private String operator;

    private final StringBuilder exportInfo = new StringBuilder();

    private String telegramInfo;

    private String telegramInfoFirst;

    private int serversLoadTimes;

    private static final int maxLoadTimes = 3; // 最大重连次数

    /**
     * 执行方法
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     * @throws Exception 执行过程中的异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, Object job) throws Exception {
        SearchHit[] hits = null;
        JSONObject urlJSON = null;
        String total = null;
        boolean url_Kibana = false;
        //判斷平台
        if(Constants.PLATFORM_JY8.equals(moniElastic.getPlatform()) || Constants.PLATFORM_PAYUB8.equals(moniElastic.getPlatform())){
            url_Kibana=true;
        }
        if(url_Kibana){
            urlJSON = SpringUtils.getBean(IMoniElasticService.class).doURLElasticSearch(moniElastic);
            total = urlJSON.getJSONObject("result").getJSONObject("rawResponse").getJSONObject("hits").getString("total");
        }else{
            SearchResponse searchResponse = SpringUtils.getBean(IMoniElasticService.class).doElasticSearch(moniElastic);
            hits = searchResponse.getHits().getHits();
        }
        //储存结果判斷URL傳total否則傳hits.length
        String result = String.format("find %s hits", url_Kibana?total:hits.length);
        moniElasticLog.setExecuteResult(result);
        if (ScheduleConstants.MATCH_NO_NEED.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
            //不等於URL Kibana跑進迴圈
        } else if (!url_Kibana && doMatch(hits,null)) {
            //处理需要导出某字段信息
            saveExportField(hits);

            Map<String, String> compareResult = null;
            if (hits.length > 0 && moniElastic.getId()==2) {
                compareResult = SpringUtils.getBean(IMoniElasticService.class).doPf1DrawCompare(hits);
                doCompare(compareResult);
            } else if (hits.length > 0 && moniElastic.getId()==1) {
                compareResult = SpringUtils.getBean(IMoniElasticService.class).doPf2DrawCompare(hits);
                doCompare(compareResult);
            } else {
                checkAndAlert();
            }
            //等於URL Kibana跑進迴圈
        } else if (url_Kibana && doMatch(null,total)) {
            //处理需要导出某字段信息
            saveUrlExportField(urlJSON,total);
            checkAndAlert();
        }else {
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        }

    }

    private void checkAndAlert() throws Exception {
        moniElasticLog.setStatus(Constants.FAIL);
        if ((!"system".equals(operator) && !isWebhook) || resultIsNotExist()) {
            //没有重复发生的LOG才发送TG告警，避免频繁发送
            if ("system".equals(operator) || isWebhook) {
                //系统执行或webhook则设置为真实告警
                moniElasticLog.setIsAlert(Constants.YES);
                moniElasticLog.setAlertStatus(Constants.SUCCESS);
                //系统执行才更新最后告警时间
                moniElastic.setLastAlert(DateUtils.getNowDate());
                SpringUtils.getBean(IMoniElasticService.class).updateMoniElasticLastAlertTime(moniElastic);
            } else {
                moniElasticLog.setIsAlert(Constants.NO);
                moniElasticLog.setAlertStatus(Constants.FAIL);
            }
            sendAlert();
            //調用API
            SpringUtils.getBean(IMoniApiService.class).doApi(moniElastic.getRelApi(), operator);
        } else {
            moniElasticLog.setIsAlert(Constants.NO);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        }
    }

    private void saveExportField(SearchHit[] hits) {
        if (hits.length > 0 && StringUtils.isNotEmpty(moniElastic.getExportField())) {
            String[] exportFields = moniElastic.getExportField().split(",");
            String sourceAsString = hits[hits.length - 1].getSourceAsString();
            JSONObject jsonObject = JSON.parseObject(sourceAsString);
            StringBuilder exportResult = new StringBuilder();
            int count = 1;
            for (String exportField : exportFields) {
                JSONObject jsonObjectTmp = jsonObject;
                String[] split = exportField.split("\\.");
                for (int i = 0; i < split.length - 1; i++) {
                    if (StringUtils.isNull(jsonObjectTmp)) {
                        break;
                    }
                    jsonObjectTmp = jsonObjectTmp.getJSONObject(split[i]);
                }
                if (exportFields.length > 1) {
                    exportResult.append("(").append(count).append(")");
                    exportInfo.append("(").append(count).append(")");
                }
                if (StringUtils.isNotNull(jsonObjectTmp)) {
                    String tmpExportResult = jsonObjectTmp.getString(split[split.length - 1]);
                    if (StringUtils.isNotEmpty(tmpExportResult)) {
                        exportResult.append(exportField).append(":").append(tmpExportResult).append("\n");
                        if (tmpExportResult.length() > 500) {
                            tmpExportResult = tmpExportResult.substring(0, 500) + "\n... See more in log details";
                        }
                        exportInfo.append(exportField).append(":").append(tmpExportResult).append("\n");
                    } else {
                        exportResult.append(exportField).append(":").append("null").append("\n");
                        exportInfo.append(exportField).append(":").append("null").append("\n");
                    }
                } else {
                    exportResult.append(exportField).append(":").append("null").append("\n");
                    exportInfo.append(exportField).append(":").append("null").append("\n");
                }
                count++;
            }
            moniElasticLog.setExportResult(exportResult.substring(0, exportResult.length() - 1));
        }
    }

    private void saveUrlExportField(JSONObject urlJSON,String total) {
        //判斷total大於0或是否有導出值
        if(Integer.parseInt(total)>0 && StringUtils.isNotEmpty(moniElastic.getExportField())) {
            //字串拆分
            String[] exportFields = moniElastic.getExportField().split(",");
            StringBuilder exportResult = new StringBuilder();
            //取得JSON內容
            JSONObject jsonObject = urlJSON.getJSONObject("result").getJSONObject("rawResponse").getJSONObject("hits").getJSONArray("hits").getJSONObject(0).getJSONObject("fields");
            int count = 1;
            for (String exportField : exportFields) {
                exportResult.append("(").append(count).append(")");
                exportInfo.append("(").append(count).append(")");
                //log紀錄
                exportResult.append(exportField+":"+jsonObject.getJSONArray(exportField)).append("\n");
                //tg推送
                exportInfo.append(exportField+":"+jsonObject.getJSONArray(exportField)).append("\n");
                count++;
            }
            moniElasticLog.setExportResult(exportResult.substring(0, exportResult.length() - 1));
        }
    }

    /**
     * 检测是否存在相同结果日志
     *
     * @return
     */
    private boolean resultIsNotExist() {
        try {
            //为0则不过滤
            if (moniElastic.getIgnoreAlert() == 0) {
                return true;
            }
            DataSource masterDataSource = SpringUtils.getBean("masterDataSource");
            String sql = "SELECT COUNT(*) FROM MONI_ELASTIC_LOG WHERE EXECUTE_RESULT = ? AND ELASTIC_ID = ? AND STATUS != '0' AND IS_ALERT = 'Y' AND START_TIME > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
            JdbcTemplate jdbcTemplateMysql = new JdbcTemplate(masterDataSource);
            int row = jdbcTemplateMysql.queryForObject(sql, new Object[]{moniElasticLog.getExecuteResult(), moniElastic.getId(), moniElastic.getIgnoreAlert()}, Integer.class);
            return row == 0;
        } catch (Exception e) {
            return true;
        }
    }

    private void doCompare(Map<String, String> compareResult) throws Exception {
        String result;
        String index = StringUtils.isNull(compareResult) ? "0" : compareResult.get("index");
        if (!"0".equals(index)) {
            result = String.format("find %s hits;\n", index) + compareResult.get("result");
            moniElasticLog.setExecuteResult(result);
            checkAndAlert();
        } else {
            result = "find 0 hits";
            moniElasticLog.setExecuteResult(result);
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        }
    }

    private void sendAlert() throws Exception {
        //发送告警
        if (Constants.SUCCESS.equals(moniElastic.getTelegramAlert())) {
            sendTelegram();
        }
    }

    /**
     * 任务执行前方法，在doExecute()方法前执行
     *
     * @param context 工作执行上下文对象
     */
    @Override
    protected void before(JobExecutionContext context, Object job) {
        moniElastic = (MoniElastic) job;
        moniElasticLog.setStartTime(new Date());
        moniElasticLog.setElasticId(moniElastic.getId());
        setExpectedResult();
        operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isEmpty(operator)) {
            operator = "system";
        }

        isWebhook = (Boolean) context.getMergedJobDataMap().get("isWebhook");
        if (StringUtils.isNull(isWebhook)) {
            isWebhook = false;
        }
        if (isWebhook) {
            operator = "webhook(" + operator + ")";
        }
        moniElasticLog.setOperator(operator);
        //此处先插入一条日志以获取日志id，方便后续使用
        SpringUtils.getBean(IMoniElasticLogService.class).addJobLog(moniElasticLog);
        //输出日志
        log.info("[Elastic检测任务]任务ID:{},任务名称:{},准备执行",
                moniElastic.getId(), moniElastic.getChName());
    }

    /**
     * 执行后方法，在doExecute()方法后执行
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    @Override
    protected void after(JobExecutionContext context, Object job, Exception e) {
        if (e != null) {
            moniElasticLog.setStatus(Constants.ERROR);
            moniElasticLog.setIsAlert(Constants.YES);
            moniElasticLog.setAlertStatus(Constants.SUCCESS);
            moniElasticLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e));
        }
    }

    /**
     * finally中执行方法
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     */
    @Override
    protected void doFinally(JobExecutionContext context, Object job) {
        moniElasticLog.setEndTime(new Date());
        long runTime = (moniElasticLog.getEndTime().getTime() - moniElasticLog.getStartTime().getTime()) / 1000;
        moniElasticLog.setExecuteTime(runTime);
        if (StringUtils.isEmpty(moniElasticLog.getExpectedResult())) {
            setExpectedResult();
        }
        //更新日志到数据库中
        SpringUtils.getBean(IMoniElasticLogService.class).updateMoniElasticLog(moniElasticLog);
        //输出日志
        log.info("[Elastic检测任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniElastic.getId(), moniElastic.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniElasticLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniElasticLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniElasticLog.getStatus()) ? "Success" : "failed");
    }

    private void setExpectedResult() {
        if (ScheduleConstants.MATCH_EQUAL.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("output = " + moniElastic.getExpectedResult());
        } else if (ScheduleConstants.MATCH_NOT_EQUAL.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("output != " + moniElastic.getExpectedResult());
        } else if (ScheduleConstants.MATCH_GREATER.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("output > " + moniElastic.getExpectedResult());
        } else if (ScheduleConstants.MATCH_LESS.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("output < " + moniElastic.getExpectedResult());
        } else if (ScheduleConstants.MATCH_NO_NEED.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("No need match");
        } else if (ScheduleConstants.MATCH_EMPTY.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("output is empty");
        } else if (ScheduleConstants.MATCH_NOT_EMPTY.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setExpectedResult("output is not empty");
        }
    }

    /**
     * 比对获取结果与预期结果
     *
     * @throws Exception
     */
    private boolean doMatch(SearchHit[] hits,String total) throws Exception {

        String autoMatch = moniElastic.getAutoMatch();

        int rows=hits!=null?hits.length:Integer.parseInt(total);

        //大于比对
        if (ScheduleConstants.MATCH_GREATER.equals(autoMatch)) {
            return rows > Integer.parseInt(moniElastic.getExpectedResult());
        }

        //小于比对
        if (ScheduleConstants.MATCH_LESS.equals(autoMatch)) {
            return rows < Integer.parseInt(moniElastic.getExpectedResult());
        }

        // 等于比对
        if (ScheduleConstants.MATCH_EQUAL.equals(autoMatch)) {
            return rows < Integer.parseInt(moniElastic.getExpectedResult());
        }

        //	不等于比对
        if (ScheduleConstants.MATCH_NOT_EQUAL.equals(autoMatch)) {
            return rows != Integer.parseInt(moniElastic.getExpectedResult());
        }

        // 无资料
        if (ScheduleConstants.MATCH_EMPTY.equals(autoMatch)) {
            return rows == 0;
        }

        // 有资料
        if (ScheduleConstants.MATCH_NOT_EMPTY.equals(autoMatch)) {
            return rows != 0;
        }

        //若是沒有進行到比對代表有問題，需拋出例外。
        throw new Exception("Never do match function");
    }


    private void sendTelegram() throws Exception {
        String[] tgData = ScheduleUtils.getTgData(moniElastic.getTelegramConfig(), isWebhook);
        String bot = tgData[0];
        String chatId = tgData[1];
        telegramInfo = moniElastic.getTelegramInfo();
        StringBuilder telegramInfoFirstBuilder = new StringBuilder();
        if (StringUtils.isNotEmpty(telegramInfo)) {
            String descr = moniElastic.getDescr();
            if (StringUtils.isNotEmpty(descr)) {
                descr = descr.replace("{id}", String.valueOf(moniElastic.getId()))
                        .replace("{asid}", moniElastic.getAsid())
                        .replace("{zh_name}", ScheduleUtils.processStr(moniElastic.getChName()))
                        .replace("{en_name}", ScheduleUtils.processStr(moniElastic.getEnName()))
                        .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniElastic.getPlatform()))
                        .replace("{kibana_url}", StringUtils.isNotEmpty(moniElastic.getKibanaUrl()) ? ScheduleUtils.processStr(moniElastic.getKibanaUrl()) : "");
            } else {
                descr = "descr is empty";
            }

            telegramInfoFirstBuilder.append("*__JobName:__*`{en_name}`/`{zh_name}`\n")
                    .append("*__MonitorID:__*`{id}`/`{asid}`\\(`{priority}`\\)\n")
                    .append("*__Operator:__*`{operator}`\\[`{platform}`/`{env}`\\]\n");

            //备用推送消息，去除descr,一般descr太长会造成推送超时，缩短推送文本长度，遇到time out时推送此文本
            telegramInfoFirst = telegramInfoFirstBuilder.toString()
                    .replace("{id}", String.valueOf(moniElastic.getId()))
                    .replace("{asid}", ScheduleUtils.processStr(moniElastic.getAsid()))
                    .replace("{priority}", "1".equals(moniElastic.getPriority()) ? "NU" : "URG")
                    .replace("{en_name}", ScheduleUtils.processStr(moniElastic.getEnName()))
                    .replace("{zh_name}", ScheduleUtils.processStr(moniElastic.getChName()))
                    .replace("{platform}", ScheduleUtils.processStr(DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniElastic.getPlatform())))
                    .replace("{operator}", operator)
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "");


            telegramInfo = telegramInfo.replace("{descr_template_elastic}", DictUtils.getDictRemark(DictTypeConstants.JOB_PUSH_TEMPLATE, Constants.DESCR_TEMPLATE_ELASTIC))
                    .replace("{id}", String.valueOf(moniElastic.getId()))
                    .replace("{asid}", ScheduleUtils.processStr(moniElastic.getAsid()))
                    .replace("{priority}", "1".equals(moniElastic.getPriority()) ? "NU" : "URG")
                    .replace("{zh_name}", ScheduleUtils.processStr(moniElastic.getChName()))
                    .replace("{en_name}", ScheduleUtils.processStr(moniElastic.getEnName()))
                    .replace("{platform}", ScheduleUtils.processStr(DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniElastic.getPlatform())))
                    .replace("{expect}", ScheduleUtils.processStr(moniElasticLog.getExpectedResult()))
                    .replace("{result}", ScheduleUtils.processStr(moniElasticLog.getExecuteResult().replace(";", "")))
                    .replace("{operator}", operator)
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "")
                    .replace("{descr}", ScheduleUtils.processStr(descr))
                    .replace("{export}", StringUtils.isEmpty(exportInfo) ? "Export field is not set" : ScheduleUtils.processStr(exportInfo.toString()))
                    .replace("{kibana_url}", StringUtils.isNotEmpty(moniElastic.getKibanaUrl()) ? ScheduleUtils.processStr(moniElastic.getKibanaUrl()) : "");

        } else {
            telegramInfo = "*LOG Monitor ID\\(" + moniElastic.getId() + "\\),Notification content is not set*";
            telegramInfoFirst = telegramInfo;
        }

        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfoFirst).parseMode(ScheduleUtils.parseMode);
        sendMessage.replyMarkup(ScheduleUtils.getInlineKeyboardMarkup(JOB_DETAIL_URL, LOG_DETAIL_URL, String.valueOf(moniElastic.getId()), String.valueOf(moniElasticLog.getId()), moniElastic.getKibanaUrl()));

        serversLoadTimes = 0;
        messageBot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
                if (response.isOk()) {
                    //记录消息id
                    Integer messageId = response.message().messageId();
                    try {
                        //继续发送正常消息
                        response = ScheduleUtils.sendMessage(bot, chatId, telegramInfo,
                                ScheduleUtils.getInlineKeyboardMarkup(JOB_DETAIL_URL, LOG_DETAIL_URL, String.valueOf(moniElastic.getId()), String.valueOf(moniElasticLog.getId()), moniElastic.getKibanaUrl()));
                        if (response.isOk()) {
                            //模板消息推送成功则删除上一个消息
                            ScheduleUtils.deleteMessage(messageBot, chatId, messageId);
                        }
                    } catch (Exception e) {
                        log.error("Log jobId：{},JobName：{},telegram推送信息异常,{}", moniElastic.getId(), moniElastic.getChName(), ExceptionUtil.getExceptionMessage(e));
                    }
                } else {
                    MoniElasticLog jobLog = new MoniElasticLog();
                    jobLog.setId(moniElasticLog.getId());
                    jobLog.setExceptionLog("Telegram send message error: ".concat(response.description()));
                    SpringUtils.getBean(IMoniElasticLogService.class).updateMoniElasticLog(jobLog);
                    log.error("Log jobId：{},JobName：{},推送内容：{},telegram发送信息失败", moniElastic.getId(), moniElastic.getChName(), telegramInfoFirst);
                }
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    TelegramBot resendBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
                    resendBot.execute(sendMessage, this);
                    log.error("Log jobId：{},JobName：{},推送内容：{},telegram信息超时重发,第{}次", moniElastic.getId(), moniElastic.getChName(), telegramInfoFirst, serversLoadTimes);
                } else {
                    String failedInfo = moniElastic.getAsid() + ": " + moniElastic.getEnName() + "/" + moniElastic.getChName();
                    try {
                        TelegramBot failedBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
                        SendMessage sendMessage = new SendMessage(chatId, failedInfo);
                        failedBot.execute(sendMessage);
                    } catch (Exception e1) {
                        MoniElasticLog jobLog = new MoniElasticLog();
                        jobLog.setId(moniElasticLog.getId());
                        jobLog.setStatus(Constants.ERROR);
                        jobLog.setExceptionLog("Telegram send message error: ".concat(ExceptionUtil.getExceptionMessage(e1)));
                        SpringUtils.getBean(IMoniElasticLogService.class).updateMoniElasticLog(jobLog);
                        log.error("Log jobId：{},JobName：{},推送内容：{},telegram发送信息异常,{}", moniElastic.getId(), moniElastic.getChName(), failedInfo, ExceptionUtil.getExceptionMessage(e1));
                    }
                }
            }
        });
    }

    /**
     * 使用toString方法构建任务名
     *
     * @return
     */
    @Override
    public String toString() {
        return JOB_CODE + "-" + id;
    }


    /**
     * 静态方法，获取一个任务执行对象
     *
     * @param moniElastic
     * @return
     */
    public static MoniElasticExecution buildJob(MoniElastic moniElastic) {
        MoniElasticExecution moniElasticExecution = new MoniElasticExecution();
        moniElasticExecution.setId(String.valueOf(moniElastic.getId()));
        moniElasticExecution.setCronExpression(moniElastic.getCronExpression());
        moniElasticExecution.setStatus(moniElastic.getStatus());
        moniElasticExecution.setJobGroup(moniElastic.getPlatform());
        moniElasticExecution.setJobContent(moniElastic);
        return moniElasticExecution;
    }
}
