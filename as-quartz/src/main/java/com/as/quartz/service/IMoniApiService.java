package com.as.quartz.service;

import com.as.quartz.domain.MoniApi;
import org.quartz.SchedulerException;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * 自动API检测任务Service接口
 *
 * @author kolin
 * @date 2021-07-26
 */
public interface IMoniApiService {
    /**
     * 查询自动API检测任务
     *
     * @param id 自动API检测任务ID
     * @return 自动API检测任务
     */
    public MoniApi selectMoniApiById(Long id);

    /**
     * 查询自动API检测任务列表
     *
     * @param moniApi 自动API检测任务
     * @return 自动API检测任务集合
     */
    public List<MoniApi> selectMoniApiList(MoniApi moniApi);

    /**
     * 新增自动API检测任务
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    public int insertMoniApi(MoniApi moniApi) throws SchedulerException;

    /**
     * 修改自动API检测任务
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    public int updateMoniApi(MoniApi moniApi) throws SchedulerException;

    /**
     * 修改自动API检测任务最后告警时间
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    public int updateMoniApiLastAlertTime(MoniApi moniApi);

    /**
     * 批量删除自动API检测任务
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public void deleteMoniApiByIds(String ids) throws SchedulerException;

    /**
     * 删除自动API检测信息
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    public int deleteMoniApi(MoniApi moniApi) throws SchedulerException;


    /**
     * 自动API检测任务状态修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeStatus(MoniApi job) throws SchedulerException;

    /**
     * 自动API检测任务告警修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeAlert(MoniApi job) throws SchedulerException;

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int pauseJob(MoniApi job) throws SchedulerException;

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int resumeJob(MoniApi job) throws SchedulerException;


    /**
     * 立即运行任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public void run(MoniApi job) throws SchedulerException;

    /**
     * HTTP模拟请求
     *
     * @param job
     * @return
     */
    public ResponseEntity<String> doUrlCheck(MoniApi job);

    /**
     * 調用Api
     *
     * @param relApi
     */
    public void doApi(String relApi) throws Exception;

    /**
     * 导入JOB数据
     *
     * @param jobList         JOB数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作人
     * @return 结果
     */
    public String importJob(List<MoniApi> jobList, Boolean isUpdateSupport, String operName);
}
