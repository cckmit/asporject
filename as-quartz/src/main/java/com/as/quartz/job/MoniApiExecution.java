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
import com.as.quartz.util.ScheduleUtils;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
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

    private final MoniApiLog moniApiLog = new MoniApiLog();

    private MoniApi moniApi = new MoniApi();

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
        ResponseEntity<String> response = SpringUtils.getBean(IMoniApiService.class).doUrlCheck(moniApi);
        //保存执行结果
        String result = response.getStatusCode().toString();
        moniApiLog.setExecuteResult(result);
        if (resultIsExist()) {
            //没有重复发生的LOG
            if (doMatch(response.getStatusCodeValue())) {
                moniApiLog.setStatus(Constants.SUCCESS);
                moniApiLog.setAlertStatus(Constants.FAIL);
            } else {
                moniApiLog.setStatus(Constants.FAIL);
                moniApiLog.setAlertStatus(Constants.SUCCESS);
                //发送告警
                if (Constants.SUCCESS.equals(moniApi.getTelegramAlert())) {
                    sendTelegram();
//                    if (!sendResponse.isOk()) {
//                        moniApiLog.setStatus(Constants.ERROR);
//                        moniApiLog.setExceptionLog("Telegram send message error: ".concat(sendResponse.description()));
//                    }
                }
                //更新最后告警时间
                moniApi.setLastAlert(DateUtils.getNowDate());
                SpringUtils.getBean(IMoniApiService.class).updateMoniApiLastAlertTime(moniApi);
            }
        } else {
            moniApiLog.setStatus(Constants.SUCCESS);
            moniApiLog.setAlertStatus(Constants.FAIL);
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
        moniApiLog.setExpectedCode(moniApi.getExpectedCode());
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
            moniApiLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e).replace("\"", "'"));
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
        long runTime = (moniApiLog.getEndTime().getTime() - moniApiLog.getStartTime().getTime()) / 1000;
        moniApiLog.setExecuteTime(runTime);
        String operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isNotEmpty(operator)) {
            moniApiLog.setOperator(operator);
        } else {
            moniApiLog.setOperator("system");
        }

        //插入日志到数据库中
        SpringUtils.getBean(IMoniApiLogService.class).addJobLog(moniApiLog);
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
    private boolean resultIsExist() {
        try {
            //为0则不过滤
            if (moniApi.getIgnoreAlert() == 0) {
                return true;
            }
            DataSource masterDataSource = SpringUtils.getBean("masterDataSource");
            String sql = "SELECT COUNT(*) FROM MONI_API_LOG WHERE EXECUTE_RESULT = ? AND API_ID = ? AND STATUS != '0' AND START_TIME > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
            JdbcTemplate jdbcTemplateMysql = new JdbcTemplate(masterDataSource);
            int row = jdbcTemplateMysql.queryForObject(sql, new Object[]{moniApiLog.getExecuteResult(), moniApi.getId(), moniApi.getIgnoreAlert()}, Integer.class);
            return row == 0;
        } catch (Exception e) {
            return true;
        }
    }


    private void sendTelegram() throws Exception {
        String[] tgData = ScheduleUtils.getTgData(moniApi.getTelegramConfig());
        String bot = tgData[0];
        String chatId = tgData[1];
        String telegramInfo = moniApi.getTelegramInfo();
        if (StringUtils.isNotEmpty(telegramInfo)) {
            telegramInfo = telegramInfo.replace("{id}", String.valueOf(moniApi.getId()))
                    .replace("{asid}", moniApi.getAsid())
                    .replace("{priority}", "1".equals(moniApi.getPriority()) ? "NU" : "URG")
                    .replace("{zh_name}", moniApi.getChName())
                    .replace("{en_name}", moniApi.getEnName())
                    .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniApi.getPlatform()))
                    .replace("{descr}", StringUtils.isNotEmpty(moniApi.getDescr()) ? moniApi.getDescr() : "")
                    .replace("{url}", moniApi.getUrl())
                    .replace("{result}", StringUtils.isNotEmpty(moniApiLog.getExecuteResult()) ? moniApiLog.getExceptionLog() : "")
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "")
                    .replace("{export}", "");
        } else {
            telegramInfo = "API Monitor ID(" + moniApi.getId() + "),Notification content is not set";
        }

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("JOB Details").url(ASConfig.getAsDomain().concat(JOB_DETAIL_URL).concat(String.valueOf(moniApi.getId()))));


        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(ScheduleUtils.okHttpClient).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfo).parseMode(ParseMode.Markdown);
        sendMessage.replyMarkup(inlineKeyboard);

        serversLoadTimes = 0;

        messageBot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
                if (!response.isOk()) {
                    MoniApiLog jobLog = new MoniApiLog();
                    jobLog.setId(moniApiLog.getId());
                    jobLog.setExceptionLog("Telegram send message error: ".concat(response.description()));
                    SpringUtils.getBean(IMoniApiLogService.class).updateMoniApiLog(jobLog);
                    log.info("{},telegram发送失败", moniApi.getChName());
                } else {
                    log.info("{},telegram发送成功", moniApi.getChName());
                }
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    messageBot.execute(sendMessage, this);
                } else {
                    MoniApiLog jobLog = new MoniApiLog();
                    jobLog.setId(moniApiLog.getId());
                    jobLog.setExceptionLog("Telegram send message error: ".concat(ExceptionUtil.getExceptionMessage(e).replace("\"", "'")));
                    SpringUtils.getBean(IMoniApiLogService.class).updateMoniApiLog(jobLog);
                    log.info("{},telegram发送异常,{}", moniApi.getChName(), ExceptionUtil.getExceptionMessage(e));
                }
            }
        });

//        return ScheduleUtils.sendMessage(bot, chatId, telegramInfo, inlineKeyboard);
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
        moniApiExecution.setJobPlatform(moniApi.getPlatform());
        moniApiExecution.setJobContent(moniApi);
        return moniApiExecution;
    }

}
