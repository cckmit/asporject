package com.as.quartz.util;

import com.as.common.constant.DictTypeConstants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.utils.DictUtils;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendDocument;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.SendResponse;
import okhttp3.OkHttpClient;
import org.quartz.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 定时任务工具类
 *
 * @author kolin
 */
@Component
public class ScheduleUtils {

    private static OkHttpClient okHttpClient;

    @PostConstruct
    public void beforeInit() {
        okHttpClient = new OkHttpClient().newBuilder().connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    /**
     * 得到quartz任务类
     *
     * @param job 执行计划
     * @return 具体执行任务类
     */
    private static Class<? extends Job> getQuartzJobClass(AbstractQuartzJob job) {
        return job.getClass();
    }

    /**
     * 构建任务触发对象
     */
    public static TriggerKey getTriggerKey(String jobId, String jobGroup) {
        return TriggerKey.triggerKey(jobId, jobGroup);
    }

    /**
     * 构建任务键对象
     */
    public static JobKey getJobKey(String jobCode, String jobGroup) {
        return JobKey.jobKey(jobCode, jobGroup);
    }

    /**
     * 创建定时任务
     */
    public static void createScheduleJob(Scheduler scheduler, AbstractQuartzJob job) throws SchedulerException {
        Class<? extends Job> jobClass = getQuartzJobClass(job);
        // 构建job信息
        String jobCode = job.toString();
        String jobGroup = job.getJobPlatform();
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobCode, jobGroup)).build();

        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());

        // 按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobCode, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job.getJobContent());

        // 判断是否存在
        if (scheduler.checkExists(getJobKey(jobCode, jobGroup))) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(getJobKey(jobCode, jobGroup));
        }

        scheduler.scheduleJob(jobDetail, trigger);

        // 暂停任务
        if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue())) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobCode, jobGroup));
        }
    }


    /**
     * 获取TG配置
     */
    public static String[] getTgData(String telegramConfig) throws Exception {
        String[] tgData;
        //如果不是生产环境则返回测试群组
        if (!"prod".equals(SpringUtils.getActiveProfile())) {
            tgData = new String[2];
            tgData[0] = "1937111623:AAHDVpT1bezDDJ_Lf7HmyYCRd8mZeSlHCwM";
            tgData[1] = "-532553117";
            return tgData;
        }

        String config = DictUtils.getDictRemark(DictTypeConstants.TELEGRAM_NOTICE_GROUP, telegramConfig);
        if (StringUtils.isEmpty(config)) {
            //若是沒有设置telegram通知群组,则抛出例外
            throw new Exception("Cant find any telegram group setting");
        }
        tgData = config.split(";");
        if (tgData.length != 2) {
            //若是数量不等于2，则配置错误
            throw new Exception("telegram group Configuration error, please check");
        }
        return tgData;
    }

    /**
     * 发送tg告警 文字
     */
    public static SendResponse sendMessage(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard) {
        TelegramBot messageBot = new TelegramBot.Builder(bot).okHttpClient(okHttpClient).build();
        SendMessage sendMessage = new SendMessage(chatId, telegramInfo).parseMode(ParseMode.Markdown);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendMessage.replyMarkup(inlineKeyboard);
        }
        return messageBot.execute(sendMessage);
    }

    /**
     * 发送tg告警 图片
     */
    public static SendResponse sendPhoto(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard, File file) {
        TelegramBot photoBot = new TelegramBot.Builder(bot).okHttpClient(okHttpClient).build();
        SendPhoto sendPhoto = new SendPhoto(chatId, file);
        sendPhoto.caption(telegramInfo).parseMode(ParseMode.Markdown);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendPhoto.replyMarkup(inlineKeyboard);
        }
        return photoBot.execute(sendPhoto);
    }

    /**
     * 发送tg告警 文件形式
     */
    public static SendResponse sendDocument(String bot, String chatId, String telegramInfo, InlineKeyboardMarkup inlineKeyboard, File file) {
        TelegramBot documentBot = new TelegramBot.Builder(bot).okHttpClient(okHttpClient).build();
        SendDocument sendDocument = new SendDocument(chatId, file);
        sendDocument.caption(telegramInfo).parseMode(ParseMode.Markdown);
        if (StringUtils.isNotNull(inlineKeyboard)) {
            sendDocument.replyMarkup(inlineKeyboard);
        }
        return documentBot.execute(sendDocument);
    }


}