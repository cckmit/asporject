package com.as.quartz.service;

import com.as.quartz.domain.MoniExport;
import org.quartz.SchedulerException;

import java.util.List;

/**
 * 自动报表任务Service接口
 *
 * @author kolin
 * @date 2021-07-23
 */
public interface IMoniExportService {
    /**
     * 查询自动报表任务
     *
     * @param id 自动报表任务ID
     * @return 自动报表任务
     */
    public MoniExport selectMoniExportById(Long id);

    /**
     * 查询自动报表任务列表
     *
     * @param moniExport 自动报表任务
     * @return 自动报表任务集合
     */
    public List<MoniExport> selectMoniExportList(MoniExport moniExport);

    /**
     * 新增自动报表任务
     *
     * @param moniExport 自动报表任务
     * @return 结果
     */
    public int insertMoniExport(MoniExport moniExport) throws SchedulerException;

    /**
     * 修改自动报表任务
     *
     * @param moniExport 自动报表任务
     * @return 结果
     */
    public int updateMoniExport(MoniExport moniExport) throws SchedulerException;

    /**
     * 修改自动报表最后导出时间
     *
     * @param moniExport 自动报表任务
     * @return 结果
     */
    public int updateMoniExportLastExportTime(MoniExport moniExport);

    /**
     * 批量删除自动报表任务
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public void deleteMoniExportByIds(String ids) throws SchedulerException;

    /**
     * 删除自动报表任务信息
     *
     * @param moniExport 自动报表任务对象
     * @return 结果
     */
    public int deleteExportJob(MoniExport moniExport) throws SchedulerException;

    /**
     * SQL检测任务状态修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeStatus(MoniExport job) throws SchedulerException;

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int pauseJob(MoniExport job) throws SchedulerException;

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int resumeJob(MoniExport job) throws SchedulerException;


    /**
     * 立即运行任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public void run(MoniExport job) throws SchedulerException;

    /**
     * 导入JOB数据
     *
     * @param jobList         JOB数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作人
     * @return 结果
     */
    public String importJob(List<MoniExport> jobList, Boolean isUpdateSupport, String operName);
}
