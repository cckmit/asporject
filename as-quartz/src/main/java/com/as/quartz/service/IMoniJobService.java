package com.as.quartz.service;

import com.as.quartz.domain.MoniJob;
import org.quartz.SchedulerException;

import java.util.List;

/**
 * SQL检测任务Service接口
 *
 * @author kolin
 * @date 2021-07-14
 */
public interface IMoniJobService {
    /**
     * 查询SQL检测任务
     *
     * @param ID SQL检测任务ID
     * @return SQL检测任务
     */
    public MoniJob selectMoniJobById(Long ID);

    /**
     * 查询SQL检测任务列表
     *
     * @param moniJob SQL检测任务
     * @return SQL检测任务集合
     */
    public List<MoniJob> selectMoniJobList(MoniJob moniJob);

    /**
     * 新增SQL检测任务
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    public int insertMoniJob(MoniJob moniJob) throws SchedulerException;

    /**
     * 修改SQL检测任务
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    public int updateMoniJob(MoniJob moniJob) throws SchedulerException;

    /**
     * 修改SQL检测任务
     *
     * @param moniJob SQL检测任务最后告警时间
     * @return 结果
     */
    public int updateMoniJobLastAlertTime(MoniJob moniJob);


    /**
     * 批量删除SQL检测任务
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public void deleteMoniJobByIds(String ids) throws SchedulerException;

    /**
     * 删除SQL检测任务信息
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    public int deleteMoniJob(MoniJob moniJob) throws SchedulerException;

    /**
     * SQL检测任务状态修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeStatus(MoniJob job) throws SchedulerException;

    /**
     * SQL检测任务告警修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeAlert(MoniJob job) throws SchedulerException;

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int pauseJob(MoniJob job) throws SchedulerException;

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int resumeJob(MoniJob job) throws SchedulerException;


    /**
     * 立即运行任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public void run(MoniJob job) throws SchedulerException;

    /**
     * 导入JOB数据
     *
     * @param jobList         JOB数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作人
     * @return 结果
     */
    public String importJob(List<MoniJob> jobList, Boolean isUpdateSupport, String operName);

}
