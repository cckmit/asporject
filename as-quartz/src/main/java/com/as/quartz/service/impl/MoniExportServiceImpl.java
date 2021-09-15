package com.as.quartz.service.impl;

import com.as.common.constant.ScheduleConstants;
import com.as.common.core.text.Convert;
import com.as.common.exception.BusinessException;
import com.as.common.utils.DateUtils;
import com.as.common.utils.MessageUtils;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.StringUtils;
import com.as.quartz.domain.MoniExport;
import com.as.quartz.job.MoniExportExecution;
import com.as.quartz.mapper.MoniExportMapper;
import com.as.quartz.service.IMoniExportService;
import com.as.quartz.util.ScheduleUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 自动报表任务Service业务层处理
 *
 * @author kolin
 * @date 2021-07-23
 */
@Service
public class MoniExportServiceImpl implements IMoniExportService {
    private static final Logger log = LoggerFactory.getLogger(MoniExportServiceImpl.class);

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private MoniExportMapper moniExportMapper;

    /**
     * 查询自动报表任务
     *
     * @param id 自动报表任务ID
     * @return 自动报表任务
     */
    @Override
    public MoniExport selectMoniExportById(Long id) {
        return moniExportMapper.selectMoniExportById(id);
    }

    /**
     * 查询自动报表任务列表
     *
     * @param moniExport 自动报表任务
     * @return 自动报表任务
     */
    @Override
    public List<MoniExport> selectMoniExportList(MoniExport moniExport) {
        Long[] jobIds = Convert.toLongArray((String) moniExport.getParams().get("ids"));
        moniExport.getParams().put("ids", jobIds);
        return moniExportMapper.selectMoniExportList(moniExport);
    }

    /**
     * 新增自动报表任务
     *
     * @param moniExport 自动报表任务
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMoniExport(MoniExport moniExport) throws SchedulerException {
        moniExport.setCreateTime(DateUtils.getNowDate());
        moniExport.setCreateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniExportMapper.insertMoniExport(moniExport);
        if (rows > 0) {
            MoniExportExecution moniExportExecution = MoniExportExecution.buildJob(moniExport);
            ScheduleUtils.createScheduleJob(scheduler, moniExportExecution);
        }

        return rows;
    }

    /**
     * 修改自动报表任务
     *
     * @param moniExport 自动报表任务
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMoniExport(MoniExport moniExport) throws SchedulerException {
        MoniExport properties = selectMoniExportById(moniExport.getId());
        moniExport.setUpdateTime(DateUtils.getNowDate());
        moniExport.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniExportMapper.updateMoniExport(moniExport);
        if (rows > 0) {
            updateSchedulerJob(moniExport, properties.getPlatform());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    private void updateSchedulerJob(MoniExport job, String jobGroup) throws SchedulerException {
        MoniExportExecution moniExportExecution = MoniExportExecution.buildJob(job);
        String jobCode = moniExportExecution.toString();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobCode, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }

        ScheduleUtils.createScheduleJob(scheduler, moniExportExecution);
    }

    @Override
    public int updateMoniExportLastExportTime(MoniExport moniExport) {
        return moniExportMapper.updateMoniExportLastExportTime(moniExport);
    }

    /**
     * 删除自动报表任务对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public void deleteMoniExportByIds(String ids) throws SchedulerException {
        Long[] jobIds = Convert.toLongArray(ids);
        for (Long jobId : jobIds) {
            MoniExport job = moniExportMapper.selectMoniExportById(jobId);
            deleteExportJob(job);
        }
    }

    /**
     * 删除SQL检测任务信息
     *
     * @param moniExport 自动报表任务对象
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteExportJob(MoniExport moniExport) throws SchedulerException {
        MoniExportExecution moniExportExecution = MoniExportExecution.buildJob(moniExport);
        String jobCode = moniExportExecution.toString();
        String jobGroup = moniExport.getPlatform();
        int rows = moniExportMapper.deleteMoniExportById(moniExport.getId());
        if (rows > 0) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobCode, jobGroup));
        }
        return rows;
    }

    /**
     * 自动报表任务状态修改
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int changeStatus(MoniExport job) throws SchedulerException {
        int rows = 0;
        String status = job.getStatus();
        job.setUpdateTime(DateUtils.getNowDate());
        job.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        if (ScheduleConstants.Status.NORMAL.getValue().equals(status)) {
            rows = resumeJob(job);
        } else if (ScheduleConstants.Status.PAUSE.getValue().equals(status)) {
            rows = pauseJob(job);
        }
        return rows;
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int pauseJob(MoniExport job) throws SchedulerException {
        MoniExportExecution moniExportExecution = new MoniExportExecution();
        moniExportExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniExportExecution.toString();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = moniExportMapper.updateMoniExport(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobCode, job.getPlatform()));
        }
        return rows;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int resumeJob(MoniExport job) throws SchedulerException {
        MoniExportExecution moniExportExecution = new MoniExportExecution();
        moniExportExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniExportExecution.toString();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = moniExportMapper.updateMoniExport(job);
        if (rows > 0) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobCode, job.getPlatform()));
        }
        return rows;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public void run(MoniExport job) throws SchedulerException {
        MoniExportExecution moniExportExecution = new MoniExportExecution();
        moniExportExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniExportExecution.toString();
        MoniExport tmpObj = selectMoniExportById(job.getId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        try {
            String operator = null;
            Map<String, Object> params = job.getParams();
            if (StringUtils.isNotEmpty(params)) {
                operator = (String) params.get("operator");
            }
            dataMap.put("operator", StringUtils.isNotEmpty(operator) ? operator : ShiroUtils.getLoginName());
        } catch (Exception e) {
            //关联导出时ShiroUtils.getLoginName()会异常，此处吞掉异常继续执行
        }

        dataMap.put(ScheduleConstants.TASK_PROPERTIES, tmpObj);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobCode, tmpObj.getPlatform()), dataMap);
    }

    /**
     * 导入JOB数据
     *
     * @param jobList         JOB数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importJob(List<MoniExport> jobList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(jobList) || jobList.size() == 0) {
            throw new BusinessException(MessageUtils.message("import.not.empty"));
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (MoniExport job : jobList) {
            try {
                // 验证是否存在这个job
                MoniExport m = moniExportMapper.selectMoniExportById(job.getId());
                if (StringUtils.isNull(m)) {
                    job.setCreateBy(operName);
                    job.setCreateTime(new Date());
                    this.insertMoniExport(job);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、JOB(" + job.getId() + ") " + job.getAsid() + " " + MessageUtils.message("import.success"));
                } else if (isUpdateSupport) {
                    job.setUpdateBy(operName);
                    job.setUpdateTime(new Date());
                    this.updateMoniExport(job);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、JOB(" + job.getId() + ") " + job.getAsid() + " " + MessageUtils.message("import.update.success"));
                } else {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、JOB(" + job.getId() + ") " + job.getAsid() + " " + MessageUtils.message("import.exist"));
                }
            } catch (Exception e) {
                failureNum++;
                String msg = "<br/>" + failureNum + "、JOB(" + job.getId() + ") " + job.getAsid() + " " + MessageUtils.message("import.failed");
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0 && successNum == 0) {
            failureMsg.insert(0, MessageUtils.message("import.failed.info", failureNum));
            throw new BusinessException(failureMsg.toString());
        } else if (failureNum > 0 && successNum > 0) {
            successMsg.insert(0, MessageUtils.message("import.success.part.info", successNum, failureNum)).append(failureMsg);
        } else {
            successMsg.insert(0, MessageUtils.message("import.success.info", successNum));
        }
        return successMsg.toString();
    }
}
