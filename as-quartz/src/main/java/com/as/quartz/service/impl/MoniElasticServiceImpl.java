package com.as.quartz.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.as.common.config.datasource.DynamicDataSourceContextHolder;
import com.as.common.constant.ScheduleConstants;
import com.as.common.core.text.Convert;
import com.as.common.enums.DataSourceType;
import com.as.common.utils.DateUtils;
import com.as.common.utils.ShiroUtils;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.job.MoniElasticExecution;
import com.as.quartz.mapper.MoniElasticMapper;
import com.as.quartz.mapper.PF1DrawCompareMapper;
import com.as.quartz.mapper.PF2DrawCompareMapper;
import com.as.quartz.service.IMoniElasticService;
import com.as.quartz.util.ScheduleUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ElasticSearch任务Service业务层处理
 *
 * @author kolin
 * @date 2021-07-28
 */
@Service
public class MoniElasticServiceImpl implements IMoniElasticService {
    private Logger logger = LoggerFactory.getLogger(MoniElasticServiceImpl.class);

    @Autowired
    private MoniElasticMapper moniElasticMapper;

    @Autowired
    private Scheduler scheduler;

    @Autowired
    @Qualifier("PF1Elasticsearch")
    private RestHighLevelClient PF1Client;

    @Autowired
    @Qualifier("PF2Elasticsearch")
    private RestHighLevelClient PF2Client;

    @Autowired
    private PF1DrawCompareMapper pf1DrawCompareMapper;

    @Autowired
    private PF2DrawCompareMapper pf2DrawCompareMapper;

    /**
     * 查询ElasticSearch任务
     *
     * @param id ElasticSearch任务ID
     * @return ElasticSearch任务
     */
    @Override
    public MoniElastic selectMoniElasticById(Long id) {
        return moniElasticMapper.selectMoniElasticById(id);
    }

    /**
     * 查询ElasticSearch任务列表
     *
     * @param moniElastic ElasticSearch任务
     * @return ElasticSearch任务
     */
    @Override
    public List<MoniElastic> selectMoniElasticList(MoniElastic moniElastic) {
        Long[] jobIds = Convert.toLongArray((String) moniElastic.getParams().get("ids"));
        moniElastic.getParams().put("ids", jobIds);
        return moniElasticMapper.selectMoniElasticList(moniElastic);
    }

    /**
     * 新增ElasticSearch任务
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    @Override
    public int insertMoniElastic(MoniElastic moniElastic) throws SchedulerException {
        moniElastic.setCreateTime(DateUtils.getNowDate());
        moniElastic.setCreateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniElasticMapper.insertMoniElastic(moniElastic);
        if (rows > 0) {
            MoniElasticExecution moniElasticExecution = MoniElasticExecution.buildJob(moniElastic);
            ScheduleUtils.createScheduleJob(scheduler, moniElasticExecution);
        }
        return rows;
    }

    /**
     * 修改ElasticSearch任务
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    @Override
    public int updateMoniElastic(MoniElastic moniElastic) throws SchedulerException {
        MoniElastic properties = selectMoniElasticById(moniElastic.getId());
        moniElastic.setUpdateTime(DateUtils.getNowDate());
        moniElastic.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniElasticMapper.updateMoniElastic(moniElastic);
        if (rows > 0) {
            updateSchedulerJob(moniElastic, properties.getPlatform());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    private void updateSchedulerJob(MoniElastic job, String jobGroup) throws SchedulerException {
        MoniElasticExecution moniElasticExecution = MoniElasticExecution.buildJob(job);
        String jobCode = moniElasticExecution.toString();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobCode, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }

        ScheduleUtils.createScheduleJob(scheduler, moniElasticExecution);
    }

    @Override
    public int updateMoniElasticLastAlertTime(MoniElastic moniElastic) {
        return moniElasticMapper.updateMoniElasticLastAlertTime(moniElastic);
    }

    /**
     * 删除ElasticSearch任务对象
     *
     * @param ids 需要删除的数据ID
     */
    @Override
    public void deleteMoniElasticByIds(String ids) throws SchedulerException {
        Long[] jobIds = Convert.toLongArray(ids);
        for (Long jobId : jobIds) {
            MoniElastic job = moniElasticMapper.selectMoniElasticById(jobId);
            deleteMoniApi(job);
        }
    }

    /**
     * 删除自动API检测任务信息
     *
     * @param moniElastic 自动API检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMoniApi(MoniElastic moniElastic) throws SchedulerException {
        MoniElasticExecution moniElasticExecution = MoniElasticExecution.buildJob(moniElastic);
        String jobCode = moniElasticExecution.toString();
        String jobGroup = moniElastic.getPlatform();
        int rows = moniElasticMapper.deleteMoniElasticById(moniElastic.getId());
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
    public int changeStatus(MoniElastic job) throws SchedulerException {
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
     * @param moniElastic 调度信息
     */
    @Override
    @Transactional
    public int changeAlert(MoniElastic moniElastic) throws SchedulerException {
        moniElastic.setUpdateTime(DateUtils.getNowDate());
        moniElastic.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniElasticMapper.updateMoniElastic(moniElastic);
        if (rows > 0) {
            MoniElastic properties = selectMoniElasticById(moniElastic.getId());
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
    public int pauseJob(MoniElastic job) throws SchedulerException {
        MoniElasticExecution moniElasticExecution = new MoniElasticExecution();
        moniElasticExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniElasticExecution.toString();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = moniElasticMapper.updateMoniElastic(job);
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
    public int resumeJob(MoniElastic job) throws SchedulerException {
        MoniElasticExecution moniElasticExecution = new MoniElasticExecution();
        moniElasticExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniElasticExecution.toString();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = moniElasticMapper.updateMoniElastic(job);
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
    public void run(MoniElastic job) throws SchedulerException {
        MoniElasticExecution moniElasticExecution = new MoniElasticExecution();
        moniElasticExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniElasticExecution.toString();
        MoniElastic tmpObj = selectMoniElasticById(job.getId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("operator", ShiroUtils.getLoginName());
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, tmpObj);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobCode, tmpObj.getPlatform()), dataMap);
    }

    @Override
    public SearchResponse doElasticSearch(MoniElastic moniElastic) throws IOException {
        SearchResponse searchResponse = null;
        if ("5.0".equals(moniElastic.getPlatform())) {
            searchResponse = doPF2(moniElastic);
        } else if ("1.0".equals(moniElastic.getPlatform())) {
            searchResponse = doPF1(moniElastic);
        }
        return searchResponse;
    }

    public SearchResponse doPF1(MoniElastic moniElastic) throws IOException {
        String query = moniElastic.getQuery();
        QueryBuilder qb = QueryBuilders.boolQuery().filter(QueryBuilders.queryStringQuery(query))
                .filter(QueryBuilders.rangeQuery("json.@timestamp").gte(moniElastic.getTimeFrom()).lte(moniElastic.getTimeTo()));
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qb);
        searchSourceBuilder.size(3);
        searchSourceBuilder.sort("json.@timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(moniElastic.getIndex());
        SearchResponse searchResponse = PF1Client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        Integer rows = hits.length;
        logger.info(String.format("[PF1] 本轮监控结果 ： find %s hits in %s by [%s]", rows, searchResponse.getTook().getStringRep(), StringUtils.isBlank(moniElastic.getAsid()) ? moniElastic.getEnName() : moniElastic.getAsid()));
        return searchResponse;
    }


    private SearchResponse doPF2(MoniElastic moniElastic) throws IOException {
        String query = moniElastic.getQuery();
        QueryBuilder qb = QueryBuilders.boolQuery()
                .filter(QueryBuilders.queryStringQuery(query))
                .filter(QueryBuilders.rangeQuery("@timestamp").gte(moniElastic.getTimeFrom()).lte(moniElastic.getTimeTo()));
        if (query.contains("redis.keyspace.avg_ttl")) {
            qb = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.queryStringQuery(query))
                    .filter(QueryBuilders.rangeQuery("@timestamp").gte(moniElastic.getTimeFrom()).lte(moniElastic.getTimeTo()))
                    .filter(QueryBuilders.rangeQuery("redis.keyspace.avg_ttl").gte(1000000000));
        }
        if (query.contains("customer-service-html")) {
            qb = QueryBuilders.boolQuery()
                    .filter(QueryBuilders.queryStringQuery(query))
                    .filter(QueryBuilders.rangeQuery("@timestamp").gte(moniElastic.getTimeFrom()).lte(moniElastic.getTimeTo()))
                    .filter(QueryBuilders.rangeQuery("stats.http_content_length").gte(500).lt(12000));
        }
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qb);
        searchSourceBuilder.size(500);
        searchSourceBuilder.sort("@timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(moniElastic.getIndex());
        SearchResponse searchResponse = PF2Client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        logger.info(String.format("PF2 本轮监控结果 ： find %s hits in %s by [%s]", hits.length, searchResponse.getTook().getStringRep(), StringUtils.isBlank(moniElastic.getAsid()) ? moniElastic.getEnName() : moniElastic.getAsid()));
        return searchResponse;
    }

    public Map<String, String> doPf1DrawCompare(SearchHit[] hits) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        Map<String, String> map = new HashMap();
        //先切换到PF1数据源
        DynamicDataSourceContextHolder.setDataSourceType(DataSourceType.PF1.name());
        for (SearchHit hit : hits) {
            try {
                String sourceAsString = hit.getSourceAsString();
                JSONObject jsonObject = JSONObject.parseObject(sourceAsString);
                JSONObject json = jsonObject.getJSONObject("json");
                String message = json.getString("message");
                String replace = message.replace("win info:", "");
                JSONObject drawInfo = JSONObject.parseObject(replace);
                String winNo = drawInfo.getString("winningNumber");
                String numero = drawInfo.getString("numero");
                String gameCode = drawInfo.getString("gameCode");
                if ("TWLKENO".equals(gameCode)) {
                    gameCode = "TWK8";
                }
                //如果未找到匹配的开奖数据则记录

                Integer count = pf1DrawCompareMapper.selectPF1DrawNumberCount(gameCode, numero, winNo);
                if (count != 1) {
                    String winNumber = pf1DrawCompareMapper.selectPF1DrawNumber(gameCode, numero);
                    result.append(String.format("========================== \nGameCode : %s \nLOG-WinningNumber : %s \nDB-WinningNumber : %s \nNumero : %s \n",
                            gameCode, winNo, winNumber, numero));
                    index++;
                } else {
                    logger.info("[PF1] Query parameters are : " + gameCode + ", " + numero + ", " + winNo + ", Result is OK: " + count);
                }
            } catch (Exception e) {
                //如果异常先清除PF1数据源
                DynamicDataSourceContextHolder.clearDataSourceType();
                throw e;
            }
        }
        map.put("index", String.valueOf(index));
        map.put("result", result.toString());
        //清除PF1数据源
        DynamicDataSourceContextHolder.clearDataSourceType();
        return map;
    }

    public Map<String, String> doPf2DrawCompare(SearchHit[] hits) {
        StringBuilder result = new StringBuilder();
        int index = 0;
        Map<String, String> map = new HashMap();
        //先切换到PF2数据源
        DynamicDataSourceContextHolder.setDataSourceType(DataSourceType.PF2_CORE.name());
        for (SearchHit hit : hits) {
            try {
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Map<String, ArrayList<String>> context = (Map<String, ArrayList<String>>) sourceAsMap.get("context");
                String winNo = context.get("win_nos").get(0);
                String numero = context.get("numeros").get(0);
                String gameCode = context.get("game_code").get(0);
                if ("SSQ".equals(gameCode)) {
                    int i = winNo.lastIndexOf(",");
                    StringBuilder sb = new StringBuilder(winNo);
                    sb.replace(i, i + 1, "-");
                    winNo = sb.toString();
                }
                //如果未找到匹配的开奖数据则记录

                int count = pf2DrawCompareMapper.selectPF2DrawNumberCount(gameCode, numero, winNo);
                if (count != 1) {
                    String winNumber = pf2DrawCompareMapper.selectPF2DrawNumber(gameCode, numero);
                    result.append(String.format("========================== \nGameCode : %s \nLOG-WinningNumber : %s \nDB-WinningNumber : %s \nNumero : %s \n", gameCode, winNo, winNumber, numero));
                    index++;
                } else {
                    logger.info("[PF2] Query parameters are : " + gameCode + ", " + numero + ", " + winNo + ", Result is OK: " + count);
                }
            } catch (Exception e) {
                //如果异常先清除PF2数据源
                DynamicDataSourceContextHolder.clearDataSourceType();
                throw e;
            }
        }
        map.put("index", String.valueOf(index));
        map.put("result", result.toString());
        //清除PF2数据源
        DynamicDataSourceContextHolder.clearDataSourceType();
        return map;
    }
}
