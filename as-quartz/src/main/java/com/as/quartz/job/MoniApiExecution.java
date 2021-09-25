package com.as.quartz.job;

import com.as.common.config.ASConfig;
import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.domain.MoniApiLog;
import com.as.quartz.service.IMoniApiLogService;
import com.as.quartz.service.IMoniApiService;
import com.as.quartz.util.AbstractQuartzJob;
import com.as.quartz.util.OkHttpUtils;
import com.as.quartz.util.ScheduleUtils;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import okhttp3.Response;
import okhttp3.ResponseBody;
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
import java.util.Objects;

/**
 * SQL检测任务执行类（禁止并发执行）
 *
 * @author kolin
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MoniApiExecution extends AbstractQuartzJob {
    private static final Logger log = LoggerFactory.getLogger(MoniApiExecution.class);

    /**
     * 创建任务名时使用的前缀，如API-JOB-1
     */
    private static final String JOB_CODE = "API-JOB";

    private static final String JOB_DETAIL_URL = "/monitor/apiJob/detail/";
    private static final String LOG_DETAIL_URL = "/monitor/apiJobLog/detail/";

    private final MoniApiLog moniApiLog = new MoniApiLog();

    private MoniApi moniApi = new MoniApi();

    private Boolean isWebhook;

    private String result;

    private String responseBody;

    private String operator;

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
        Response response = null;
        try {
            response = SpringUtils.getBean(IMoniApiService.class).doUrlCheck(moniApi);
            //执行结果
            result = String.valueOf(response.code());
            ResponseBody body = response.body();
            if (StringUtils.isNotNull(body)) {
                responseBody = body.string();
            } else {
                responseBody = "null";
            }
        } catch (Exception e) {
            //执行异常结果
            result = ExceptionUtil.getRootErrorMessage(e);
            responseBody = "null";
            moniApiLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e));
        }
        //保存执行结果
        moniApiLog.setExecuteResult(result);
        moniApiLog.setResponse(responseBody);
        if (StringUtils.isNotNull(response) && doMatch(response.code())) {
            moniApiLog.setStatus(Constants.SUCCESS);
            moniApiLog.setAlertStatus(Constants.FAIL);
        } else {
            moniApiLog.setStatus(Constants.FAIL);
            if ((!"system".equals(operator) && !isWebhook) || resultIsNotExist()) {
                //没有重复发生的LOG 发送告警
                if ("system".equals(operator) || isWebhook) {
                    ////系统执行或webhook则设置为真实告警
                    moniApiLog.setIsAlert(Constants.YES);
                    moniApiLog.setAlertStatus(Constants.SUCCESS);
                    //系统执行才更新最后告警时间
                    moniApi.setLastAlert(DateUtils.getNowDate());
                    SpringUtils.getBean(IMoniApiService.class).updateMoniApiLastAlertTime(moniApi);
                } else {
                    moniApiLog.setIsAlert(Constants.NO);
                    moniApiLog.setAlertStatus(Constants.FAIL);
                }

                if (Constants.SUCCESS.equals(moniApi.getTelegramAlert())) {
                    sendTelegram();
                }
            } else {
                moniApiLog.setIsAlert(Constants.NO);
                moniApiLog.setAlertStatus(Constants.FAIL);
            }
        }
    }

    /**
     * 任务执行前方法，在doExecute()方法前执行
     *
     * @param context 工作执行上下文对象
     */
    @Override
    protected void before(JobExecutionContext context, Object job) {
        moniApi = (MoniApi) job;
        moniApiLog.setStartTime(new Date());
        moniApiLog.setApiId(moniApi.getId());

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
        moniApiLog.setOperator(operator);
        //此处先插入一条日志以获取日志id，方便后续使用
        SpringUtils.getBean(IMoniApiLogService.class).addJobLog(moniApiLog);
        //输出日志
        log.info("[API检测任务]任务ID:{},任务名称:{},准备执行",
                moniApi.getId(), moniApi.getChName());
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
            moniApiLog.setStatus(Constants.ERROR);
            moniApiLog.setAlertStatus(Constants.SUCCESS);
            moniApiLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e));
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
        moniApiLog.setEndTime(new Date());
        moniApiLog.setExpectedCode(moniApi.getExpectedCode());
        long runTime = (moniApiLog.getEndTime().getTime() - moniApiLog.getStartTime().getTime()) / 1000;
        moniApiLog.setExecuteTime(runTime);
        //更新日志到数据库中
        SpringUtils.getBean(IMoniApiLogService.class).updateMoniApiLog(moniApiLog);
        //输出日志
        log.info("[API检测任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniApi.getId(), moniApi.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniApiLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniApiLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniApiLog.getStatus()) ? "Success" : "failed");
    }

    /**
     * 比对获取结果与预期结果
     *
     * @throws Exception
     */
    private boolean doMatch(Integer statusCode) {
        String expectedCode = moniApi.getExpectedCode();
        String[] expectedCodes = expectedCode.split(",");
        for (String code : expectedCodes) {
            if (statusCode == Integer.parseInt(code)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测是否存在相同结果日志
     *
     * @return
     */
    private boolean resultIsNotExist() {
        try {
            //为0则不过滤
            if (moniApi.getIgnoreAlert() == 0) {
                return true;
            }
            DataSource masterDataSource = SpringUtils.getBean("masterDataSource");
            String sql = "SELECT COUNT(*) FROM MONI_API_LOG WHERE EXECUTE_RESULT = ? AND API_ID = ? AND STATUS != '0' AND IS_ALERT = 'Y' AND START_TIME > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
            JdbcTemplate jdbcTemplateMysql = new JdbcTemplate(masterDataSource);
            int row = jdbcTemplateMysql.queryForObject(sql, new Object[]{moniApiLog.getExecuteResult(), moniApi.getId(), moniApi.getIgnoreAlert()}, Integer.class);
            return row == 0;
        } catch (Exception e) {
            return true;
        }
    }


    private void sendTelegram() throws Exception {
        String[] tgData = ScheduleUtils.getTgData(moniApi.getTelegramConfig(), isWebhook);
        String bot = tgData[0];
        String chatId = tgData[1];
        telegramInfo = moniApi.getTelegramInfo();
        StringBuilder telegramInfoFirstBuilder = new StringBuilder();
        if (StringUtils.isNotEmpty(telegramInfo)) {
            String descr = moniApi.getDescr();
            if (StringUtils.isNotEmpty(descr)) {
                descr = descr.replace("{id}", String.valueOf(moniApi.getId()))
                        .replace("{asid}", moniApi.getAsid())
                        .replace("{zh_name}", ScheduleUtils.processStr(moniApi.getChName()))
                        .replace("{en_name}", ScheduleUtils.processStr(moniApi.getEnName()))
                        .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniApi.getPlatform()));
            } else {
                descr = "descr is empty";
            }
            if (responseBody.length() > 500) {
                responseBody = responseBody.substring(0, 500) + "... See more in log details";
            }

            telegramInfoFirstBuilder.append("*__JobName:__*`{en_name}`/`{zh_name}`\n")
                    .append("*__MonitorID:__*`{id}`/`{asid}`\\(`{priority}`\\)\n")
                    .append("*__Operator:__*`{operator}`\\[`{platform}`/`{env}`\\]");

            //备用推送消息，去除descr,response,一般descr,response太长会造成推送超时，缩短推送文本长度，遇到time out时推送此文本
            telegramInfoFirst = telegramInfoFirstBuilder.toString()
                    .replace("{id}", String.valueOf(moniApi.getId()))
                    .replace("{asid}", ScheduleUtils.processStr(moniApi.getAsid()))
                    .replace("{priority}", "1".equals(moniApi.getPriority()) ? "NU" : "URG")
                    .replace("{en_name}", ScheduleUtils.processStr(moniApi.getEnName()))
                    .replace("{zh_name}", ScheduleUtils.processStr(moniApi.getChName()))
                    .replace("{platform}", ScheduleUtils.processStr(DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniApi.getPlatform())))
                    .replace("{operator}", operator)
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "");

            telegramInfo = telegramInfo.replace("{descr_template_api}", DictUtils.getDictRemark(DictTypeConstants.JOB_PUSH_TEMPLATE, Constants.DESCR_TEMPLATE_API))
                    .replace("{id}", String.valueOf(moniApi.getId()))
                    .replace("{asid}", ScheduleUtils.processStr(moniApi.getAsid()))
                    .replace("{priority}", "1".equals(moniApi.getPriority()) ? "NU" : "URG")
                    .replace("{zh_name}", ScheduleUtils.processStr(moniApi.getChName()))
                    .replace("{en_name}", ScheduleUtils.processStr(moniApi.getEnName()))
                    .replace("{platform}", ScheduleUtils.processStr(DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniApi.getPlatform())))
                    .replace("{url}", ScheduleUtils.processStr(moniApi.getUrl()))
                    .replace("{expect}", moniApi.getExpectedCode())
                    .replace("{result}", result)
                    .replace("{operator}", operator)
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "")
                    .replace("{descr}", ScheduleUtils.processStr(descr))
                    .replace("{response}", ScheduleUtils.processStr(responseBody));

        } else {
            telegramInfo = "*API Monitor ID\\(" + moniApi.getId() + "\\),Notification content is not set*";
            telegramInfoFirst = telegramInfo;
        }

        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfoFirst).parseMode(ScheduleUtils.parseMode);
        sendMessage.replyMarkup(ScheduleUtils.getInlineKeyboardMarkup(JOB_DETAIL_URL, LOG_DETAIL_URL, String.valueOf(moniApi.getId()), String.valueOf(moniApiLog.getId())));
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
                                ScheduleUtils.getInlineKeyboardMarkup(JOB_DETAIL_URL, LOG_DETAIL_URL, String.valueOf(moniApi.getId()), String.valueOf(moniApiLog.getId())));
                        if (response.isOk()) {
                            //正常消息推送成功则删除上一个消息
                            ScheduleUtils.deleteMessage(messageBot, chatId, messageId);
                        }
                    } catch (Exception e) {
                        log.error("API jobId：{},JobName：{},telegram推送信息异常,{}", moniApi.getId(), moniApi.getChName(), ExceptionUtil.getExceptionMessage(e));
                    }
                } else {
                    MoniApiLog jobLog = new MoniApiLog();
                    jobLog.setId(moniApiLog.getId());
                    jobLog.setExceptionLog("Telegram send message error: ".concat(response.description()));
                    SpringUtils.getBean(IMoniApiLogService.class).updateMoniApiLog(jobLog);
                    log.error("API jobId：{},JobName：{},推送内容：{},telegram发送信息失败", moniApi.getId(), moniApi.getChName(), telegramInfoFirst);
                }
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    TelegramBot resendBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
                    resendBot.execute(sendMessage, this);
                    log.error("API jobId：{},JobName：{},推送内容：{},telegram信息超时重发,第{}次", moniApi.getId(), moniApi.getChName(), telegramInfoFirst, serversLoadTimes);
                } else {
                    try {
                        TelegramBot failedBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
                        String failedInfo = moniApi.getAsid() + ":" + moniApi.getEnName() + "/" + moniApi.getChName()
                                + "\nExe time: " + DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniApiLog.getStartTime())
                                + "\nLOG Details:\n" + ASConfig.getAsDomain().concat(LOG_DETAIL_URL).concat(String.valueOf(moniApiLog.getId()));
                        SendMessage sendMessage = new SendMessage(chatId, failedInfo);
                        failedBot.execute(sendMessage);
                    } catch (Exception e1) {
                        MoniApiLog jobLog = new MoniApiLog();
                        jobLog.setId(moniApiLog.getId());
                        jobLog.setStatus(Constants.ERROR);
                        jobLog.setExceptionLog("Telegram send message error: ".concat(ExceptionUtil.getExceptionMessage(e)));
                        SpringUtils.getBean(IMoniApiLogService.class).updateMoniApiLog(jobLog);
                        log.error("API jobId：{},JobName：{},推送内容：{},telegram发送信息异常,{}", moniApi.getId(), moniApi.getChName(), telegramInfoFirst, ExceptionUtil.getExceptionMessage(e1));
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
     * @param moniApi
     * @return
     */
    public static MoniApiExecution buildJob(MoniApi moniApi) {
        MoniApiExecution moniApiExecution = new MoniApiExecution();
        moniApiExecution.setId(String.valueOf(moniApi.getId()));
        moniApiExecution.setCronExpression(moniApi.getCronExpression());
        moniApiExecution.setStatus(moniApi.getStatus());
        moniApiExecution.setJobGroup(moniApi.getPlatform());
        moniApiExecution.setJobContent(moniApi);
        return moniApiExecution;
    }

}
