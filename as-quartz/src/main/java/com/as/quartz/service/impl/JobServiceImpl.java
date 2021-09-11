package com.as.quartz.service.impl;

import com.as.common.utils.MessageUtils;
import com.as.common.utils.StringUtils;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.domain.MoniExport;
import com.as.quartz.domain.MoniJob;
import com.as.quartz.job.MoniApiExecution;
import com.as.quartz.job.MoniElasticExecution;
import com.as.quartz.job.MoniExportExecution;
import com.as.quartz.job.MoniJobExecution;
import com.as.quartz.mapper.MoniApiMapper;
import com.as.quartz.mapper.MoniElasticMapper;
import com.as.quartz.mapper.MoniExportMapper;
import com.as.quartz.mapper.MoniJobMapper;
import com.as.quartz.service.IJobService;
import com.as.quartz.util.CronUtils;
import com.as.quartz.util.Mail;
import com.as.quartz.util.ScheduleUtils;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.sql.DataSource;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定时任务调度 公共服务层
 *
 * @author kolin
 */
@Service
@DependsOn("flywayConfig")
public class JobServiceImpl implements IJobService {
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private MoniJobMapper moniJobMapper;

    @Autowired
    private MoniExportMapper moniExportMapper;

    @Autowired
    private MoniApiMapper moniApiMapper;

    @Autowired
    private MoniElasticMapper moniElasticMapper;

    @Autowired
    private JavaMailSender javaMailSender;


    @Autowired
    @Qualifier("masterDataSource")
    DataSource masterDataSource;

    @Autowired
    @Qualifier("pf1DataSource")
    DataSource pf1DataSource;

    @Autowired
    @Qualifier("pf1SecDataSource")
    DataSource pf1SecDataSource;

    @Autowired
    @Qualifier("pf2CoreDataSource")
    DataSource pf2CoreDataSource;

    @Autowired
    @Qualifier("pf2CoreSecDataSource")
    DataSource pf2CoreSecDataSource;

    @Autowired
    @Qualifier("pf2DrawDataSource")
    DataSource pf2DrawDataSource;

    @Autowired
    @Qualifier("pf2DwDataSource")
    DataSource pf2DwDataSource;

    Map<String, JdbcTemplate> jdbcMap = new HashMap<String, JdbcTemplate>();

    /**
     * 项目启动时，初始化定时器
     * 主要是防止手动修改数据库导致未同步到定时任务处理（注：不能手动修改数据库ID和任务组名，否则会导致脏数据）
     */
    @PostConstruct
    public void init() throws SchedulerException {
        scheduler.clear();
        List<MoniJob> moniJobs = moniJobMapper.selectMoniJobAll();
        for (MoniJob moniJob : moniJobs) {
            MoniJobExecution moniJobExecution = MoniJobExecution.buildJob(moniJob);
            ScheduleUtils.createScheduleJob(scheduler, moniJobExecution);
        }
        List<MoniExport> exports = moniExportMapper.selectMoniExportAll();
        for (MoniExport export : exports) {
            MoniExportExecution moniExportExecution = MoniExportExecution.buildJob(export);
            ScheduleUtils.createScheduleJob(scheduler, moniExportExecution);
        }
        List<MoniApi> apis = moniApiMapper.selectMoniApiAll();
        for (MoniApi api : apis) {
            MoniApiExecution moniApiExecution = MoniApiExecution.buildJob(api);
            ScheduleUtils.createScheduleJob(scheduler, moniApiExecution);
        }
        List<MoniElastic> elastics = moniElasticMapper.selectMoniElasticAll();
        for (MoniElastic elastic : elastics) {
            MoniElasticExecution moniElasticExecution = MoniElasticExecution.buildJob(elastic);
            ScheduleUtils.createScheduleJob(scheduler, moniElasticExecution);
        }
        setjdbcTemplateMap();
    }

    /**
     * 校验cron表达式是否有效
     *
     * @param cronExpression 表达式
     * @return 结果
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression) {
        return CronUtils.isValid(cronExpression);
    }

    @Override
    public boolean sqlTest(String script, String jdbc) {
        String sql;
        if (jdbc.toUpperCase().indexOf("MYSQL") >= 0) {
            sql = "SELECT COUNT(0) FROM ( " + script + ") t limit 1";
        } else {
            sql = "SELECT COUNT(0) FROM ( " + script + ") WHERE ROWNUM =1";
        }
        jdbcMap.get(jdbc.trim()).queryForList(sql);
        return true;
    }

    private void setjdbcTemplateMap() {
        jdbcMap.put("mysql-as-portal", new JdbcTemplate(masterDataSource));
        jdbcMap.put("ub8-pf1", new JdbcTemplate(pf1DataSource));
        jdbcMap.put("ub8-pf1-sec", new JdbcTemplate(pf1SecDataSource));
        jdbcMap.put("ub8-pf5-core", new JdbcTemplate(pf2CoreDataSource));
        jdbcMap.put("ub8-pf5-core-sec", new JdbcTemplate(pf2CoreSecDataSource));
        jdbcMap.put("ub8-pf5-draw", new JdbcTemplate(pf2DrawDataSource));
        jdbcMap.put("data-warehouse", new JdbcTemplate(pf2DwDataSource));
    }

    public Map<String, JdbcTemplate> getJdbcMap() {
        return jdbcMap;
    }

    /**
     * 发送邮件
     *
     * @return
     */
    @Override
    public void sendEmail(Mail mail) throws MessagingException, UnsupportedEncodingException {

        //	step1.	mail參數設定
        MimeMessage mailMessage = javaMailSender.createMimeMessage();
        MimeMessageHelper messageHelper = new MimeMessageHelper(mailMessage, true, "UTF-8");

        //寄信人
        messageHelper.setFrom(new InternetAddress(((JavaMailSenderImpl) javaMailSender).getUsername(), "AS-Portal"));

        //	step2.	設定寄信資訊
        //收信人
        if (StringUtils.isNotEmpty(mail.getTo())) {
            messageHelper.setTo(mail.getTo());
        }
        //收件人副本
        if (StringUtils.isNotEmpty(mail.getCc())) {
            messageHelper.setCc(mail.getCc());
        }
        //收件人密件副本
        if (StringUtils.isNotEmpty(mail.getBcc())) {
            messageHelper.setBcc(mail.getBcc());
        }
        //附件
        if (mail.getAttachment() != null) {
            FileSystemResource fileSystemResource = new FileSystemResource(mail.getAttachment());
            messageHelper.addAttachment(mail.getAttachment().getName(), fileSystemResource);
        }
        //主題
        messageHelper.setSubject(mail.getSubject());
        //內容
        if (mail.getContent() != null) {
            messageHelper.setText(mail.getContent());
        }

        //寄信F
        javaMailSender.send(mailMessage);
    }

    @Override
    public String getCronSchdule(String cron, int count) {
        List<String> cronSchdule = CronUtils.getCronSchdule(cron, count);
        StringBuilder times = new StringBuilder();
        times.append(MessageUtils.message("job.view.recent.run.time", cron)).append("<br/>");
        int num = 1;
        for (String time : cronSchdule) {
            times.append(num);
            if (num < 10) {
                times.append("、&nbsp;&nbsp;");
            } else {
                times.append("、&nbsp;");
            }
            times.append(time).append("<br/>");
            num++;
        }
        return times.toString();
    }
}