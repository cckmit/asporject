package com.as.quartz.job;

import com.as.common.config.ASConfig;
import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniExport;
import com.as.quartz.domain.MoniJob;
import com.as.quartz.domain.MoniJobLog;
import com.as.quartz.service.*;
import com.as.quartz.util.AbstractQuartzJob;
import com.as.quartz.util.HtmlTemplateUtil;
import com.as.quartz.util.OkHttpUtils;
import com.as.quartz.util.ScheduleUtils;
import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.*;
import com.pengrad.telegrambot.response.SendResponse;
import gui.ava.html.image.generator.HtmlImageGenerator;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;
import java.io.File;
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
public class MoniJobExecution extends AbstractQuartzJob {
    private static final Logger log = LoggerFactory.getLogger(MoniJobExecution.class);

    /**
     * 创建任务名时使用的前缀，如SQL-JOB-1
     */
    private static final String JOB_CODE = "SQL-JOB";

    private static final String LOG_DETAIL_URL = "/monitor/sqlJobLog/detail/";
    private static final String JOB_DETAIL_URL = "/monitor/sqlJob/detail/";

    private MoniJobLog moniJobLog = new MoniJobLog();

    private MoniJob moniJob = new MoniJob();

    private int serversLoadTimes;

    private static final int maxLoadTimes = 3; // 最大重连次数

    private String bot;

    private String chatId;

    private Integer messageId;

    private File file;

    private String telegramInfo;

    private SendMessage sendMessage;

    /**
     * 执行方法
     *
     * @param context 工作执行上下文对象
     * @param job     系统计划任务
     * @throws Exception 执行过程中的异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, Object job) throws Exception {
        Map<String, JdbcTemplate> jdbcMap = SpringUtils.getBean(IJobService.class).getJdbcMap();
        SqlRowSet rowSet = jdbcMap.get(moniJob.getJdbc().trim()).queryForRowSet(moniJob.getScript());
        String exeResult = getResultTable(rowSet);
        log.info("执行结果:{}", exeResult);
        moniJobLog.setExecuteResult(exeResult);
        if (resultIsExist()) {
            //没有重复发生的LOG
            if (ScheduleConstants.MATCH_NO_NEED.equals(moniJob.getAutoMatch())) {
                moniJobLog.setStatus(Constants.SUCCESS);
                moniJobLog.setAlertStatus(Constants.FAIL);
            } else if (doMatch(rowSet)) {
                moniJobLog.setStatus(Constants.SUCCESS);
                moniJobLog.setAlertStatus(Constants.FAIL);
            } else {
                moniJobLog.setStatus(Constants.FAIL);
                moniJobLog.setAlertStatus(Constants.SUCCESS);
                if (Constants.SUCCESS.equals(moniJob.getTelegramAlert())) {
                    sendTelegram();
                }
                //更新最后告警时间
                moniJob.setLastAlert(DateUtils.getNowDate());
                SpringUtils.getBean(IMoniJobService.class).updateMoniJobLastAlertTime(moniJob);
                //关联导出
                doExport(moniJob.getRelExport());
                //調用API
                SpringUtils.getBean(IMoniApiService.class).doApi(moniJob.getRelApi());
            }
        } else {
            moniJobLog.setStatus(Constants.SUCCESS);
            moniJobLog.setAlertStatus(Constants.FAIL);
        }
    }

    /**
     * 任务执行前方法，在doExecute()方法前执行
     *
     * @param context 工作执行上下文对象
     */
    @Override
    protected void before(JobExecutionContext context, Object job) {
        moniJob = (MoniJob) job;
        moniJobLog.setStartTime(new Date());
        moniJobLog.setJobId(moniJob.getId());
        setExpectedResult();
        //此处先插入一条日志以获取日志id，方便后续使用
        SpringUtils.getBean(IMoniJobLogService.class).addJobLog(moniJobLog);
        //输出日志
        log.info("[SQL检测任务]任务ID:{},任务名称:{},准备执行",
                moniJob.getId(), moniJob.getChName());
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
            moniJobLog.setStatus(Constants.ERROR);
            moniJobLog.setAlertStatus(Constants.SUCCESS);
            moniJobLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e));
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
        moniJobLog.setEndTime(new Date());
        long runTime = (moniJobLog.getEndTime().getTime() - moniJobLog.getStartTime().getTime()) / 1000;
        moniJobLog.setExecuteTime(runTime);
        String operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isNotEmpty(operator)) {
            moniJobLog.setOperator(operator);
        } else {
            moniJobLog.setOperator("system");
        }
        if (StringUtils.isEmpty(moniJobLog.getExpectedResult())) {
            setExpectedResult();
        }
        //之前已经插入,本次更新日志到数据库中
        SpringUtils.getBean(IMoniJobLogService.class).updateJobLog(moniJobLog);
        //输出日志
        log.info("[SQL检测任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniJob.getId(), moniJob.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniJobLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniJobLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniJobLog.getStatus()) ? "Success" : "failed");
    }

    private void setExpectedResult() {
        if (ScheduleConstants.MATCH_EQUAL.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("output = " + moniJob.getExpectedResult());
        } else if (ScheduleConstants.MATCH_NOT_EQUAL.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("output != " + moniJob.getExpectedResult());
        } else if (ScheduleConstants.MATCH_GREATER.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("output > " + moniJob.getExpectedResult());
        } else if (ScheduleConstants.MATCH_LESS.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("output < " + moniJob.getExpectedResult());
        } else if (ScheduleConstants.MATCH_NO_NEED.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("No need match");
        } else if (ScheduleConstants.MATCH_EMPTY.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("output is empty");
        } else if (ScheduleConstants.MATCH_NOT_EMPTY.equals(moniJob.getAutoMatch())) {
            moniJobLog.setExpectedResult("output is not empty");
        }
    }

    /**
     * 将执行结果封装成HTML表格
     *
     * @param rowSet
     * @return
     */
    private String getResultTable(SqlRowSet rowSet) {
        String field;
        Object obj;
        StringBuilder builder = new StringBuilder();

        //取得栏位名
        String[] fields = rowSet.getMetaData().getColumnNames();

        //拼接表头
        builder.append("<table style=\"width:100%;\" class=\"data_table\"><thead><tr>");
        for (int i = 0; i < fields.length; i++) {
            builder.append("<th>").append(fields[i]).append("</th>");
        }
        builder.append("</tr></thead>");

        // 拼接数据资料
        builder.append("<tbody>");
        while (rowSet.next()) {
            builder.append("<tr>");
            for (int j = 0; j < fields.length; j++) {
                obj = rowSet.getObject(fields[j]);

                //          將查詢結果格式化
                //          1.Null : -
                //          2.String :
                //          3.Date : yyyy-MM-dd HH:mm:ss
                //          4.Boolean : true, false
                //          5.BigDecimal : -
                if (obj == null) {
                    field = "-";
                } else if (obj instanceof String) {
                    field = obj.toString();
                } else if (obj instanceof Date) {
                    field = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, (Date) obj);
                } else if (obj instanceof Boolean) {
                    field = ((Boolean) obj).toString();
                } else {
                    field = obj.toString();
                }
                builder.append("<td>").append(field).append("</td>");
            }
            builder.append("</tr>");
            //数据超过60000则省略
            if (builder.length() > 60000) {
                builder.append("<tr><td colspan=\"").append(fields.length).append("\">More data can not be showed......</td></tr>");
                break;
            }
        }
        builder.append("</tbody></table>");

        return builder.toString();
    }

    /**
     * 检测是否存在相同结果日志
     *
     * @return
     */
    private boolean resultIsExist() {
        try {
            //为0则不过滤
            if (moniJob.getIgnoreAlert() == 0) {
                return true;
            }
            DataSource masterDataSource = SpringUtils.getBean("masterDataSource");
            String sql = "SELECT COUNT(*) FROM MONI_JOB_LOG WHERE EXECUTE_RESULT = ? AND JOB_ID = ? AND START_TIME > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
            JdbcTemplate jdbcTemplateMysql = new JdbcTemplate(masterDataSource);
            int row = jdbcTemplateMysql.queryForObject(sql, new Object[]{moniJobLog.getExecuteResult(), moniJob.getId(), moniJob.getIgnoreAlert()}, Integer.class);
            return row == 0;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 比对获取结果与预期结果
     *
     * @throws Exception
     */
    private boolean doMatch(SqlRowSet rowSet) throws Exception {

        String autoMatch = moniJob.getAutoMatch();
        String[] expected = moniJob.getExpectedResult().split("@");
        Double expectedDouble;
        Double resultDouble;
        Object field;

        //获取栏位名
        String[] fields = rowSet.getMetaData().getColumnNames();

        //将游标移动到第一行
        rowSet.first();

        //基本比对
        if ((ScheduleConstants.MATCH_GREATER.equals(autoMatch) || ScheduleConstants.MATCH_LESS.equals(autoMatch))
                && (fields.length != 1 || expected.length != 1)) {
            //栏位不止一格却做大于或小于比对，直接返回false
            moniJobLog.setExpectedResult(moniJobLog.getExpectedResult().concat("\n(栏位或预期结果不止一个,无法做大于和小于比对\n\"Greater than\" and \"Less than\" operator only effect in number format and only one field.)"));
            return false;
        } else if (expected.length != fields.length
                && (ScheduleConstants.MATCH_EQUAL.equals(autoMatch)
                || ScheduleConstants.MATCH_NOT_EQUAL.equals(autoMatch))) {
            //等于不等于比较，栏位数量不等于预期结果数量，直接返回false
            moniJobLog.setExpectedResult(moniJobLog.getExpectedResult().concat("\n(栏位或预期结果数量不相等,无法做不等于和等于比对\nThe number of fields and expected result are not equal,cant do \"unequal\" and \"equal\".)"));
            return false;
        }

        //大于比对
        if (ScheduleConstants.MATCH_GREATER.equals(autoMatch)) {
            expectedDouble = Double.valueOf(expected[0]);
            do {
                resultDouble = Double.valueOf(Objects.requireNonNull(rowSet.getString(fields[0])));
                if (resultDouble.compareTo(expectedDouble) < 0) {
                    return false;
                }
            } while (rowSet.next());
            return true;
        }

        //小于比对
        if (ScheduleConstants.MATCH_LESS.equals(autoMatch)) {
            expectedDouble = Double.valueOf(expected[0]);
            do {
                resultDouble = Double.valueOf(Objects.requireNonNull(rowSet.getString(fields[0])));
                if (resultDouble.compareTo(expectedDouble) > 0) {
                    return false;
                }
            }
            while (rowSet.next());
            return true;
        }

        // 等于比对
        if (ScheduleConstants.MATCH_EQUAL.equals(autoMatch)) {
            do {
                for (int i = 0; i < fields.length; i++) {
                    field = rowSet.getObject(fields[i]) != null ? Objects.requireNonNull(rowSet.getObject(fields[i])).toString() : "";
                    if (!expected[i].equals(field)) {
                        return false;
                    }
                }
            }
            while (rowSet.next());
            return true;
        }

        //	不等于比对
        if (ScheduleConstants.MATCH_NOT_EQUAL.equals(autoMatch)) {
            do {
                for (int i = 0; i < fields.length; i++) {
                    field = rowSet.getObject(fields[i]) != null ? Objects.requireNonNull(rowSet.getObject(fields[i])).toString() : "";
                    if (expected[i].equals(field)) {
                        return false;
                    }
                }
            } while (rowSet.next());
            return true;
        }

        // 无资料
        if (ScheduleConstants.MATCH_EMPTY.equals(autoMatch)) {
            return rowSet.getRow() == 0;
        }

        // 有资料
        if (ScheduleConstants.MATCH_NOT_EMPTY.equals(autoMatch)) {
            return rowSet.getRow() != 0;
        }

        //若是沒有進行到比對代表有問題，需拋出例外。
        throw new Exception("Never do match function");
    }

    /**
     * 关联导出
     *
     * @param relExport
     */
    private void doExport(String relExport) throws Exception {
        if (StringUtils.isNotEmpty(relExport)) {
            IMoniExportService moniExportService = SpringUtils.getBean(IMoniExportService.class);
            String[] ids = relExport.split(",");
            for (String id : ids) {
                MoniExport moniExport = moniExportService.selectMoniExportById(Long.parseLong(id));
                if (StringUtils.isNotNull(moniExport)) {
                    moniExportService.run(moniExport);
                } else {
                    throw new Exception("The related export job does not exist");
                }

            }
        }
    }

    private void sendTelegram() throws Exception {
        String[] tgData = ScheduleUtils.getTgData(moniJob.getTelegramConfig());
        bot = tgData[0];
        chatId = tgData[1];
        telegramInfo = moniJob.getTelegramInfo();
        if (StringUtils.isNotEmpty(telegramInfo)) {
            telegramInfo = telegramInfo.replace("{id}", String.valueOf(moniJob.getId()))
                    .replace("{asid}", moniJob.getAsid())
                    .replace("{priority}", "1".equals(moniJob.getPriority()) ? "NU" : "URG")
                    .replace("{zh_name}", moniJob.getChName())
                    .replace("{en_name}", moniJob.getEnName())
                    .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniJob.getPlatform()))
                    .replace("{descr}", StringUtils.isNotEmpty(moniJob.getDescr()) ? moniJob.getDescr() : "")
                    .replace("{result}", "")
                    .replace("{env}", StringUtils.isNotEmpty(SpringUtils.getActiveProfile()) ? Objects.requireNonNull(SpringUtils.getActiveProfile()) : "")
                    .replace("{export}", "");
        } else {
            telegramInfo = "DB Monitor ID(" + moniJob.getId() + "),Notification content is not set";
        }

        String imgPath = createImg();

        InlineKeyboardMarkup inlineKeyboard = new InlineKeyboardMarkup(
                new InlineKeyboardButton("JOB Details").url(ASConfig.getAsDomain().concat(JOB_DETAIL_URL).concat(String.valueOf(moniJob.getId()))),
                new InlineKeyboardButton("LOG Details").url(ASConfig.getAsDomain().concat(LOG_DETAIL_URL).concat(String.valueOf(moniJobLog.getId()))));


        file = new File(imgPath);
        BufferedImage bufferedImage;
        int width = 1501;
        int height = 1501;
        try {
            bufferedImage = ImageIO.read(file);
            width = bufferedImage.getWidth();
            height = bufferedImage.getHeight();
        } catch (Exception e) {
            //异常忽略，继续往下执行
        }

        TelegramBot telegramBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();

        sendMessage = new SendMessage(chatId, telegramInfo).parseMode(ParseMode.Markdown);
        sendMessage.replyMarkup(inlineKeyboard);
        try {
            SendResponse response = ScheduleUtils.sendMessage(bot, chatId, telegramInfo, inlineKeyboard);
            if (response.isOk()) {
                messageId = response.message().messageId();
            }
        } catch (Exception e) {

        }

        //图片长宽不超过1500则发送图片，否则发送附件
        if (width <= 1500 && height <= 1500) {
            SendPhoto sendPhoto = new SendPhoto(chatId, file);
            sendPhoto.caption(telegramInfo).parseMode(ParseMode.Markdown);
            sendPhoto.replyMarkup(inlineKeyboard);
            sendPhoto(telegramBot, sendPhoto);
        } else {
            SendDocument sendDocument = new SendDocument(chatId, file);
            sendDocument.caption(telegramInfo).parseMode(ParseMode.Markdown);
            sendDocument.replyMarkup(inlineKeyboard);
            sendDocument(telegramBot, sendDocument);
        }
    }

    private void sendPhoto(TelegramBot photoBot, SendPhoto sendPhoto) {
        serversLoadTimes = 5;
        photoBot.execute(sendPhoto, new Callback<SendPhoto, SendResponse>() {
            @Override
            public void onResponse(SendPhoto request, SendResponse response) {
                if (response.isOk()) {
                    MoniJobLog jobLog = new MoniJobLog();
                    jobLog.setId(moniJobLog.getId());
                    jobLog.setStatus(Constants.FAIL);
                    SpringUtils.getBean(IMoniJobLogService.class).updateJobLog(jobLog);
                    deleteMessage(photoBot);
                } else {
                    //图片文件发送失败则发送文字消息
                    if (StringUtils.isNull(messageId)) {
                        sendMessage();
                    }
                    log.error("DB jobId：{},JobName：{},telegram发送图片失败", moniJob.getId(), moniJob.getChName());
                }
            }

            @Override
            public void onFailure(SendPhoto request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    photoBot.execute(sendPhoto, this);
                    log.error("DB jobId：{},JobName：{},telegram图片超时重发,第{}次", moniJob.getId(), moniJob.getChName(), serversLoadTimes);
                } else {
                    //图片文件发送失败则发送文字消息
                    if (StringUtils.isNull(messageId)) {
                        sendMessage();
                    }
                    log.error("DB jobId：{},JobName：{},telegram发送图片异常,{}", moniJob.getId(), moniJob.getChName(), ExceptionUtil.getExceptionMessage(e));
                }
            }
        });
    }

    private void sendDocument(TelegramBot documentBot, SendDocument sendDocument) {
        serversLoadTimes = 5;
        documentBot.execute(sendDocument, new Callback<SendDocument, SendResponse>() {
            @Override
            public void onResponse(SendDocument request, SendResponse response) {
                if (response.isOk()) {
                    MoniJobLog jobLog = new MoniJobLog();
                    jobLog.setId(moniJobLog.getId());
                    jobLog.setStatus(Constants.FAIL);
                    SpringUtils.getBean(IMoniJobLogService.class).updateJobLog(jobLog);
                    deleteMessage(documentBot);
                } else {
                    //图片文件发送失败则发送文字消息
                    if (StringUtils.isNull(messageId)) {
                        sendMessage();
                    }
                    log.error("DB jobId：{},JobName：{},telegram发送附件失败", moniJob.getId(), moniJob.getChName());
                }
            }

            @Override
            public void onFailure(SendDocument request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    documentBot.execute(sendDocument, this);
                    log.error("DB jobId：{},JobName：{},telegram附件超时重发,第{}次", moniJob.getId(), moniJob.getChName(), serversLoadTimes);
                } else {
                    //图片文件发送失败则发送文字消息
                    if (StringUtils.isNull(messageId)) {
                        sendMessage();
                    }
                    log.error("DB jobId：{},JobName：{},telegram发送附件异常,{}", moniJob.getId(), moniJob.getChName(), ExceptionUtil.getExceptionMessage(e));
                }
            }
        });
    }

    private void deleteMessage(TelegramBot bot) {
        if (StringUtils.isNotNull(messageId)) {
            telegramInfo = telegramInfo + "\n\n" + "*This message will be deleted in 5 seconds*";
            EditMessageText editMessageText = new EditMessageText(chatId, messageId, telegramInfo).parseMode(ParseMode.Markdown);
            bot.execute(editMessageText);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            DeleteMessage deleteMessage = new DeleteMessage(chatId, messageId);
            bot.execute(deleteMessage);
        }
    }

    private void sendMessage() {
        serversLoadTimes = 0;
        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(OkHttpUtils.getInstance()).build();
        messageBot.execute(sendMessage, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
                if (!response.isOk()) {
                    moniJobLog.setExceptionLog("Telegram send message error: ".concat(response.description()));
                    SpringUtils.getBean(IMoniJobLogService.class).updateJobLog(moniJobLog);
                    log.error("DB jobId：{},JobName：{},telegram发送信息失败", moniJob.getId(), moniJob.getChName());
                }
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                //失败重发
                if (e instanceof SocketTimeoutException && serversLoadTimes < maxLoadTimes) {
                    serversLoadTimes++;
                    messageBot.execute(sendMessage, this);
                    log.error("DB jobId：{},JobName：{},telegram信息超时重发,第{}次", moniJob.getId(), moniJob.getChName(), serversLoadTimes);
                } else {
                    moniJobLog.setStatus(Constants.ERROR);
                    moniJobLog.setExceptionLog("Telegram send message error: ".concat(ExceptionUtil.getExceptionMessage(e).replace("\"", "'")));
                    SpringUtils.getBean(IMoniJobLogService.class).updateJobLog(moniJobLog);
                    log.error("DB jobId：{},JobName：{},telegram发送信息异常,{}", moniJob.getId(), moniJob.getChName(), ExceptionUtil.getExceptionMessage(e));
                }
            }
        });
    }

    private String createImg() {
        String htmlContent = HtmlTemplateUtil.getHtmlContent("vm/sqlJob.html.vm");
        String path = null;
        if (StringUtils.isNotEmpty(htmlContent)) {
            try {
                //替换模板数据
                htmlContent = htmlContent.replace("{descr}", moniJob.getDescr())
                        .replace("{startTime}", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniJobLog.getStartTime()))
                        .replace("{expectedResult}", StringUtils.isNull(moniJob.getExpectedResult()) ? "" : moniJob.getExpectedResult())
                        .replace("{executeResult}", moniJobLog.getExecuteResult());
                HtmlImageGenerator imageGenerator = new HtmlImageGenerator();
                imageGenerator.loadHtml(htmlContent);
                imageGenerator.getBufferedImage();
                path = HtmlTemplateUtil.getPath(DateUtils.datePath() + File.separator + new Date().getTime() + ".png");
                imageGenerator.saveAsImage(path);
            } catch (Exception e) {
                log.error("创建图片发生异常", e);
            }
        }
        return path;
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
     * @param moniJob
     * @return
     */
    public static MoniJobExecution buildJob(MoniJob moniJob) {
        MoniJobExecution moniJobExecution = new MoniJobExecution();
        moniJobExecution.setId(String.valueOf(moniJob.getId()));
        moniJobExecution.setCronExpression(moniJob.getCronExpression());
        moniJobExecution.setStatus(moniJob.getStatus());
        moniJobExecution.setJobGroup(moniJob.getPlatform());
        moniJobExecution.setJobContent(moniJob);
        return moniJobExecution;
    }

}
