package com.as.quartz.job;

import com.as.common.config.ASConfig;
import com.as.common.constant.Constants;
import com.as.common.utils.DateUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniExport;
import com.as.quartz.domain.MoniExportLog;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniExportLogService;
import com.as.quartz.service.IMoniExportService;
import com.as.quartz.util.AbstractQuartzJob;
import com.as.quartz.util.ExcelUtil;
import com.as.quartz.util.Mail;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * Export任务执行类（禁止并发执行）
 *
 * @author kolin
 */
@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class MoniExportExecution extends AbstractQuartzJob {
    private static final Logger log = LoggerFactory.getLogger(MoniExportExecution.class);

    /**
     * 创建任务名时使用的前缀，如EXPORT-JOB-1
     */
    private static final String JOB_CODE = "EXPORT-JOB";

    private MoniExportLog moniExportLog = new MoniExportLog();

    private MoniExport moniExport = new MoniExport();

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
        SqlRowSet rowSet = jdbcMap.get(moniExport.getJdbc().trim()).queryForRowSet(moniExport.getScript());
        File file = export(rowSet, moniExport.getTicketNumber());
        moniExportLog.setFileName(file.getName());

        Mail mail = new Mail();
        if (!"dev".equals(SpringUtils.getActiveProfile())) {
            mail.setTo(moniExport.getMailTo().split(";"));
            if (org.apache.commons.lang3.StringUtils.isNotBlank(moniExport.getMailCc())) {
                mail.setCc(moniExport.getMailCc().split(";"));
            }
            if (org.apache.commons.lang3.StringUtils.isNotBlank(moniExport.getMailBcc())) {
                mail.setBcc(moniExport.getMailBcc().split(";"));
            }
        } else {
            String[] testMail = new String[1];
            testMail[0] = "c98fb80a.my-cpg.com@apac.teams.ms";
            mail.setTo(testMail);
            mail.setCc(null);
            mail.setBcc(null);
        }


        //发送邮件
        mail.setSubject("[Export Data] " + moniExport.getMailSubject());
        mail.setContent(moniExport.getMailContent());
        mail.setAttachment(file);
        SpringUtils.getBean(IJobService.class).sendEmail(mail);

        moniExportLog.setStatus(Constants.SUCCESS);
        //更新最后导出时间
        moniExport.setLastExport(DateUtils.getNowDate());
        SpringUtils.getBean(IMoniExportService.class).updateMoniExportLastExportTime(moniExport);
    }

    /**
     * 任务执行前方法，在doExecute()方法前执行
     *
     * @param context 工作执行上下文对象
     */
    @Override
    protected void before(JobExecutionContext context, Object job) {
        moniExport = (MoniExport) job;
        moniExportLog.setStartTime(new Date());
        moniExportLog.setExportId(moniExport.getId());
        //输出日志
        log.info("[自动报表任务]任务ID:{},任务名称:{},准备执行",
                moniExport.getId(), moniExport.getChName());
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
            moniExportLog.setStatus(Constants.ERROR);
            moniExportLog.setExceptionLog(ExceptionUtil.getExceptionMessage(e).replace("\"", "'"));
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
        moniExportLog.setEndTime(new Date());
        long runTime = (moniExportLog.getEndTime().getTime() - moniExportLog.getStartTime().getTime()) / 1000;
        moniExportLog.setExecuteTime(runTime);
        String operator = (String) context.getMergedJobDataMap().get("operator");
        if (StringUtils.isNotEmpty(operator)) {
            moniExportLog.setOperator(operator);
        } else {
            moniExportLog.setOperator("system");
        }

        //插入日志到数据库中
        SpringUtils.getBean(IMoniExportLogService.class).addExportLog(moniExportLog);
        //输出日志
        log.info("[自动报表任务]任务ID:{},任务名称:{},开始时间:{},结束时间:{},执行结束,耗时：{}秒,执行状态:{}",
                moniExport.getId(), moniExport.getChName(), DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniExportLog.getStartTime()),
                DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, moniExportLog.getEndTime()), runTime, Constants.SUCCESS.equals(moniExportLog.getStatus()) ? "Success" : "failed");
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
     * 將資料匯出成excel file
     *
     * @param rowSet       - 導出數據
     * @param ticketNumber - 單號
     * @return 匯出的excel file
     */
    private File export(SqlRowSet rowSet, String ticketNumber) throws Exception {
        File file;
        //1.有單號：/path/ASI-XXXX_yyyyMMddHHmmss.xls
        //2.無單號：/path/yyyyMMddHHmmss.xls
        if (StringUtils.isNotBlank(ticketNumber)) {
            file = new File(ASConfig.getExcelPath() + File.separator + ticketNumber + "_" + DateUtils.dateTimeNow() + ".xlsx");
        } else {
            file = new File(ASConfig.getExcelPath() + File.separator + DateUtils.dateTimeNow() + ".xlsx");
        }
        //目录如果不存在，则创建
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
        }

        ExcelUtil excelHelper = ExcelUtil.getInstance();
        excelHelper.writeExcel(file, rowSet);

        return file;
    }

    /**
     * 静态方法，获取一个任务执行对象
     *
     * @param moniExport
     * @return
     */
    public static MoniExportExecution buildJob(MoniExport moniExport) {
        MoniExportExecution moniExportExecution = new MoniExportExecution();
        moniExportExecution.setId(String.valueOf(moniExport.getId()));
        moniExportExecution.setCronExpression(moniExport.getCronExpression());
        moniExportExecution.setStatus(moniExport.getStatus());
        moniExportExecution.setJobGroup(moniExport.getPlatform());
        moniExportExecution.setJobContent(moniExport);
        return moniExportExecution;
    }

}
