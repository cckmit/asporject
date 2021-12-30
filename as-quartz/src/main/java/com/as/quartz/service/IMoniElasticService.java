package com.as.quartz.service;

import com.alibaba.fastjson.JSONObject;
import com.as.quartz.domain.MoniElastic;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.quartz.SchedulerException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * ElasticSearch任务Service接口
 *
 * @author kolin
 * @date 2021-07-28
 */
public interface IMoniElasticService {
    /**
     * 查询ElasticSearch任务
     *
     * @param id ElasticSearch任务ID
     * @return ElasticSearch任务
     */
    public MoniElastic selectMoniElasticById(Long id);

    /**
     * 查询ElasticSearch任务列表
     *
     * @param moniElastic ElasticSearch任务
     * @return ElasticSearch任务集合
     */
    public List<MoniElastic> selectMoniElasticList(MoniElastic moniElastic);

    /**
     * 新增ElasticSearch任务
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    public int insertMoniElastic(MoniElastic moniElastic) throws SchedulerException;

    /**
     * 修改ElasticSearch任务
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    public int updateMoniElastic(MoniElastic moniElastic) throws SchedulerException;

    /**
     * 修改ElasticSearch任务最后告警时间
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    public int updateMoniElasticLastAlertTime(MoniElastic moniElastic);

    /**
     * 批量删除ElasticSearch任务
     *
     * @param ids 需要删除的数据ID
     */
    public void deleteMoniElasticByIds(String ids) throws SchedulerException;

    /**
     * 删除ElasticSearch信息
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    public int deleteMoniApi(MoniElastic moniElastic) throws SchedulerException;


    /**
     * ElasticSearch任务状态修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeStatus(MoniElastic job) throws SchedulerException;

    /**
     * ElasticSearch任务告警修改
     *
     * @param job 调度信息
     * @return 结果
     */
    public int changeAlert(MoniElastic job) throws SchedulerException;

    /**
     * 暂停任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int pauseJob(MoniElastic job) throws SchedulerException;

    /**
     * 恢复任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public int resumeJob(MoniElastic job) throws SchedulerException;


    /**
     * 立即运行任务
     *
     * @param job 调度信息
     * @return 结果
     */
    public void run(MoniElastic job) throws SchedulerException;


    /**
     * 执行ElasticSearch
     *
     * @param job 调度信息
     * @return 结果
     */
    public SearchResponse doElasticSearch(MoniElastic job) throws SchedulerException, IOException;


    public Map<String, String> doPf1DrawCompare(SearchHit[] hits);


    public Map<String, String> doPf2DrawCompare(SearchHit[] hits);

    /**
     * 导入JOB数据
     *
     * @param jobList         JOB数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作人
     * @return 结果
     */
    public String importJob(List<MoniElastic> jobList, Boolean isUpdateSupport, String operName);


    public int updateTemplate(String template);

    /**
     * 执行URLElasticSearch
     *
     * @param job 调度信息
     * @return 结果
     */
    public JSONObject doURLElasticSearch(MoniElastic job) throws IOException;
}
