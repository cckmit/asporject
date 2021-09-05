package com.as.quartz.job;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.as.common.config.ASConfig;
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
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
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

    private MoniElasticLog moniElasticLog = new MoniElasticLog();

    private MoniElastic moniElastic = new MoniElastic();

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
        SearchResponse searchResponse = SpringUtils.getBean(IMoniElasticService.class).doElasticSearch(moniElastic);
        SearchHit[] hits = searchResponse.getHits().getHits();
        //储存结果
        String result = String.format("find %s hits", hits.length);
        moniElasticLog.setExecuteResult(result);
        if (ScheduleConstants.MATCH_NO_NEED.equals(moniElastic.getAutoMatch())) {
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        } else if (doMatch(hits)) {
            //处理需要导出某字段信息
            saveExportField(hits);

            Map<String, String> compareResult = null;
            if (hits.length > 0 && hits[0].getSourceAsString().contains("win info")) {
                compareResult = SpringUtils.getBean(IMoniElasticService.class).doPf1DrawCompare(hits);
                doCompare(compareResult);
            } else if (hits.length > 0 && hits[0].getSourceAsString().contains("win_nos")) {
                compareResult = SpringUtils.getBean(IMoniElasticService.class).doPf2DrawCompare(hits);
                doCompare(compareResult);
            } else {
                checkAndAlert();
            }
        } else {
            moniElasticLog.setStatus(Constants.SUCCESS);
            moniElasticLog.setAlertStatus(Constants.FAIL);
        }

    }

    private void checkAndAlert() throws Exception {
        if (resultIsExist()) {
            //没有重复发生的LOG才发送TG告警，避免频繁发送
            moniElasticLog.setStatus(Constants.FAIL);
            moniElasticLog.setAlertStatus(Constants.SUCCESS);
            sendAlert();
            //更新最后告警时间
            moniElastic.setLastAlert(DateUtils.getNowDate());
            SpringUtils.getBean(IMoniElasticService.class).updateMoniElasticLastAlertTime(moniElastic);
            //調用API
            SpringUtils.getBean(IMoniApiService.class).doApi(moniElastic.getRelApi());
        } else {
            moniElasticLog.setStatus(Constants.SUCCESS);
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
                }
                if (StringUtils.isNotNull(jsonObjectTmp)) {
                    String tmpExportResult = jsonObjectTmp.getString(split[split.length - 1]);
                    if (StringUtils.isNotEmpty(tmpExportResult)) {
                        if (tmpExportResult.length() > 500) {
                            tmpExportResult = tmpExportResult.substring(0, 500) + "\n... more data not be showed";
                        }
                        exportResult.append(exportField).append(":").append(tmpExportResult).append("\n\n");
                    } else {
                        exportResult.append(exportField).append(":").append("null").append("\n\n");
                    }
                } else {
                    exportResult.append(exportField).append(":").append("null").append("\n\n");
                }
                count++;
            }
            moniElasticLog.setExportResult(exportResult.substring(0, exportResult.length() - 2));
        }
    }

    /**
     * 检测是否存在相同结果日志
     *
     * @return
     */
    private boolean resultIsExist() {
        try {
            //为0则不过滤
            if (moniElastic.getIgnoreAlert() == 0) {
                return true;
            }
            DataSource masterDataSource = SpringUtils.getBean("masterDataSource");
            String sql = "SELECT COUNT(*) FROM MONI_ELASTIC_LOG WHERE EXECUTE_RESULT = ? AND ELASTIC_ID = ? AND STATUS != '0' AND START_TIME > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
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
            moniElasticLog.setAlertStatus(Constants.SUCCESS);
            moniElasticLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e).replace("\"", "'"));
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
        String operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isNotEmpty(operator)) {
            moniElasticLog.setOperator(operator);
        } else {
            moniElasticLog.setOperator("system");
        }
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
        //更新日志到数据库中
        SpringUtils.getBean(IMoniElasticLogService.class).updateMoniElasticLog(moniElasticLog);
        //输出日志
        log.info("[Elastic检测任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniElastic.getId(), moniElastic.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniElasticLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniElasticLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniElasticLog.getStatus()) ? "Success" : "failed");
    }

    /**
     * 比对获取结果与预期结果
     *
     * @throws Exception
     */
    private boolean doMatch(SearchHit[] hits) throws Exception {

        String autoMatch = moniElastic.getAutoMatch();

        int rows = hits.length;

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
        String[] tgData = ScheduleUtils.getTgData(moniElastic.getTelegramConfig());
        String bot = tgData[0];
        String chatId = tgData[1];
        String telegramInfo = moniElastic.getTelegramInfo();
        if (StringUtils.isNotEmpty(telegramInfo)) {
            telegramInfo = telegramInfo.replace("{id}", String.valueOf(moniElastic.getId()))
                    .replace("{asid}", moniElastic.getAsid())
                    .replace("{priority}", "1".equals(moniElastic.getPriority()) ? "NU" : "URG")
                    .replace("{zh_name}", moniElastic.getChName())
                    .replace("{en_name}", moniElastic.getEnName())
                    .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniElastic.getPlatform()))
                    .replace("{descr}", StringUtils.isNotEmpty(moniElastic.getDescr()) ? moniElastic.getDescr() : "")
                    .replace("{result}", moniElasticLog.getExecuteResult().replace(";", ""))
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "")
                    .replace("{export}", StringUtils.isNull(moniElasticLog.getExportResult()) ? "" : moniElasticLog.getExportResult());
        } else {
            telegramInfo = "LOG Monitor ID(" + moniElastic.getId() + "),Notification content is not set";
        }

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("JOB Details").url(ASConfig.getAsDomain().concat(JOB_DETAIL_URL).concat(String.valueOf(moniElastic.getId()))),
                new InlineKeyboardButton("LOG Details").url(ASConfig.getAsDomain().concat(LOG_DETAIL_URL).concat(String.valueOf(moniElasticLog.getId()))));


        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfo).parseMode(ParseMode.Markdown);
        sendMessage.replyMarkup(inlineKeyboard);

        serversLoadTimes = 0;

        messageBot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
                if (!response.isOk()) {
                    moniElasticLog.setExceptionLog("Telegram send message error: ".concat(response.description()));
                    SpringUtils.getBean(IMoniElasticLogService.class).updateMoniElasticLog(moniElasticLog);
                    log.error("Log jobId：{},JobName：{},telegram发送信息失败", moniElastic.getId(), moniElastic.getChName());
                }
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    messageBot.execute(sendMessage, this);
                    log.error("Log jobId：{},JobName：{},telegram信息超时重发,第{}次", moniElastic.getId(), moniElastic.getChName(), serversLoadTimes);
                } else {
                    log.info("{},telegram发送失败,{}", moniElastic.getChName(), ExceptionUtil.getExceptionMessage(e));
                    moniElasticLog.setStatus(Constants.ERROR);
                    moniElasticLog.setExceptionLog("Telegram send message error: ".concat(ExceptionUtil.getExceptionMessage(e).replace("\"", "'")));
                    SpringUtils.getBean(IMoniElasticLogService.class).updateMoniElasticLog(moniElasticLog);
                    log.error("Log jobId：{},JobName：{},telegram发送信息异常,{}", moniElastic.getId(), moniElastic.getChName(), ExceptionUtil.getExceptionMessage(e));
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
