package com.as.quartz.service.impl;

import com.as.common.constant.ScheduleConstants;
import com.as.common.core.text.Convert;
import com.as.common.exception.BusinessException;
import com.as.common.utils.DateUtils;
import com.as.common.utils.MessageUtils;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.StringUtils;
import com.as.quartz.domain.MoniJob;
import com.as.quartz.job.MoniJobExecution;
import com.as.quartz.mapper.MoniJobMapper;
import com.as.quartz.service.IMoniJobService;
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
 * SQL检测任务Service业务层处理
 *
 * @author kolin
 * @date 2021-07-14
 */
@Service
public class MoniJobServiceImpl implements IMoniJobService {
    private static final Logger log = LoggerFactory.getLogger(MoniJobServiceImpl.class);

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private MoniJobMapper moniJobMapper;

    /**
     * 查询SQL检测任务
     *
     * @param ID SQL检测任务ID
     * @return SQL检测任务
     */
    @Override
    public MoniJob selectMoniJobById(Long ID) {
        return moniJobMapper.selectMoniJobById(ID);
    }

    /**
     * 查询SQL检测任务列表
     *
     * @param moniJob SQL检测任务
     * @return SQL检测任务
     */
    @Override
    public List<MoniJob> selectMoniJobList(MoniJob moniJob) {
        Long[] jobIds = Convert.toLongArray((String) moniJob.getParams().get("ids"));
        moniJob.getParams().put("ids", jobIds);
        return moniJobMapper.selectMoniJobList(moniJob);
    }

    /**
     * 新增SQL检测任务
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMoniJob(MoniJob moniJob) throws SchedulerException {
        moniJob.setCreateTime(DateUtils.getNowDate());
        moniJob.setCreateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniJobMapper.insertMoniJob(moniJob);
        if (rows > 0) {
            MoniJobExecution moniJobExecution = MoniJobExecution.buildJob(moniJob);
            ScheduleUtils.createScheduleJob(scheduler, moniJobExecution);
        }
        return rows;
    }

    /**
     * 修改SQL检测任务
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMoniJob(MoniJob moniJob) throws SchedulerException {
        MoniJob properties = selectMoniJobById(moniJob.getId());
        moniJob.setUpdateTime(DateUtils.getNowDate());
        moniJob.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniJobMapper.updateMoniJob(moniJob);
        if (rows > 0) {
            updateSchedulerJob(moniJob, properties.getPlatform());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    private void updateSchedulerJob(MoniJob job, String jobGroup) throws SchedulerException {
        MoniJobExecution moniJobExecution = MoniJobExecution.buildJob(job);
        String jobCode = moniJobExecution.toString();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobCode, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }

        ScheduleUtils.createScheduleJob(scheduler, moniJobExecution);
    }

    @Override
    public int updateMoniJobLastAlertTime(MoniJob moniJob) {
        return moniJobMapper.updateMoniJobLastAlertTime(moniJob);
    }

    /**
     * 删除SQL检测任务对象  删除任务后，所对应的trigger也将被删除
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public void deleteMoniJobByIds(String ids) throws SchedulerException {
        Long[] jobIds = Convert.toLongArray(ids);
        for (Long jobId : jobIds) {
            MoniJob job = moniJobMapper.selectMoniJobById(jobId);
            deleteMoniJob(job);
        }
    }

    /**
     * 删除SQL检测任务信息
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMoniJob(MoniJob moniJob) throws SchedulerException {
        MoniJobExecution moniJobExecution = MoniJobExecution.buildJob(moniJob);
        String jobCode = moniJobExecution.toString();
        String jobGroup = moniJob.getPlatform();
        int rows = moniJobMapper.deleteMoniJobById(moniJob.getId());
        if (rows > 0) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobCode, jobGroup));
        }
        return rows;
    }

    /**
     * SQL检测任务状态修改
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int changeStatus(MoniJob job) throws SchedulerException {
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
     * SQL检测任务告警状态修改
     *
     * @param moniJob 调度信息
     */
    @Override
    @Transactional
    public int changeAlert(MoniJob moniJob) throws SchedulerException {
        moniJob.setUpdateTime(DateUtils.getNowDate());
        moniJob.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniJobMapper.updateMoniJob(moniJob);
        if (rows > 0) {
            MoniJob properties = selectMoniJobById(moniJob.getId());
            updateSchedulerJob(properties, properties.getPlatform());
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
    public int pauseJob(MoniJob job) throws SchedulerException {
        MoniJobExecution moniJobExecution = new MoniJobExecution();
        moniJobExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniJobExecution.toString();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = moniJobMapper.updateMoniJob(job);
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
    public int resumeJob(MoniJob job) throws SchedulerException {
        MoniJobExecution moniJobExecution = new MoniJobExecution();
        moniJobExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniJobExecution.toString();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = moniJobMapper.updateMoniJob(job);
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
    public void run(MoniJob job) throws SchedulerException {
        MoniJobExecution moniJobExecution = new MoniJobExecution();
        moniJobExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniJobExecution.toString();
        MoniJob tmpObj = selectMoniJobById(job.getId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        try {
            Boolean isWebhook = false;
            Map<String, Object> params = job.getParams();
            if (StringUtils.isNotEmpty(params)) {
                isWebhook = (Boolean) params.get("isWebhook");
            }
            dataMap.put("isWebhook", isWebhook);
            dataMap.put("operator", ShiroUtils.getLoginName());
        } catch (Exception e) {
            // do nothing
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
    public String importJob(List<MoniJob> jobList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(jobList) || jobList.size() == 0) {
            throw new BusinessException(MessageUtils.message("import.not.empty"));
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (MoniJob job : jobList) {
            try {
                // 验证是否存在这个job
                MoniJob m = moniJobMapper.selectMoniJobById(job.getId());
                if (StringUtils.isNull(m)) {
                    job.setCreateBy(operName);
                    job.setCreateTime(new Date());
                    this.insertMoniJob(job);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、JOB(" + job.getId() + ") " + job.getAsid() + " " + MessageUtils.message("import.success"));
                } else if (isUpdateSupport) {
                    job.setUpdateBy(operName);
                    job.setUpdateTime(new Date());
                    this.updateMoniJob(job);
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
