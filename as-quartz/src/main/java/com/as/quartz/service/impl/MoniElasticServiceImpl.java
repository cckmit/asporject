package com.as.quartz.service.impl;

import com.alibaba.fastjson.*;
import com.as.common.config.datasource.DynamicDataSourceContextHolder;
import com.as.common.constant.Constants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.core.text.Convert;
import com.as.common.enums.DataSourceType;
import com.as.common.exception.BusinessException;
import com.as.common.utils.DateUtils;
import com.as.common.utils.MessageUtils;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.StringUtils;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.job.MoniElasticExecution;
import com.as.quartz.mapper.MoniElasticMapper;
import com.as.quartz.mapper.PF1DrawCompareMapper;
import com.as.quartz.mapper.PF2DrawCompareMapper;
import com.as.quartz.service.IMoniElasticService;
import com.as.quartz.util.ScheduleUtils;
import okhttp3.*;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

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
    private PF1DrawCompareMapper pf1DrawCompareMapper;

    @Autowired
    private PF2DrawCompareMapper pf2DrawCompareMapper;

    /**
     * pf1 url
     */
    @Value("${elastic.pf1.url}")
    private String pf1Url;

    /**
     * pf1 port
     */
    @Value("${elastic.pf1.port}")
    private int pf1Port;

    /**
     * pf2 url
     */
    @Value("${elastic.pf2.url}")
    private String pf2Url;

    /**
     * pf2 port
     */
    @Value("${elastic.pf2.port}")
    private int pf2Port;

    /**
     * jy8 url
     */
    @Value("${elastic.jy8.url}")
    private String jy8Url;
    /**
     * payub8 url
     */
    @Value("${elastic.payub8.url}")
    private String payub8Url;

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
        try {
            Boolean isWebhook = false;
            String operator = null;
            Map<String, Object> params = job.getParams();
            if (StringUtils.isNotEmpty(params)) {
                isWebhook = (Boolean) params.get("isWebhook");
                operator = (String) params.get("operator");
            }
            dataMap.put("isWebhook", isWebhook);
            dataMap.put("operator", StringUtils.isNotEmpty(operator) ? operator : ShiroUtils.getLoginName());
        } catch (Exception e) {
            // do nothing
        }
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, tmpObj);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobCode, tmpObj.getPlatform()), dataMap);
    }

    @Override
    public SearchResponse doElasticSearch(MoniElastic moniElastic) throws IOException {
        SearchResponse searchResponse = null;
        RestHighLevelClient client = null;
        try {
            if (Constants.PLATFORM_2.equals(moniElastic.getPlatform())) {
                client = createClient(pf2Url, pf2Port);
                searchResponse = doPF2(moniElastic, client);
            } else if (Constants.PLATFORM_1.equals(moniElastic.getPlatform())) {
                client = createClient(pf1Url, pf1Port);
                searchResponse = doPF1(moniElastic, client);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (StringUtils.isNotNull(client)) {
                client.close();
            }
        }

        return searchResponse;
    }
    //取得kibana url
    private RestHighLevelClient createClient(String url, int port) {
        return new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost(url, port, "http"))
                        .setRequestConfigCallback(requestConfigBuilder -> {
                            return requestConfigBuilder.setConnectTimeout(5000 * 1000) // 连接超时（默认为1秒）
                                    .setSocketTimeout(6000 * 1000)// 套接字超时（默认为30秒）//更改客户端的超时限制默认30秒现在改为100分钟
                                    .setConnectionRequestTimeout(5000 * 1000); //请求超时
                        }));
    }

    public SearchResponse doPF1(MoniElastic moniElastic, RestHighLevelClient client) throws IOException {
        String query = moniElastic.getQuery();
        QueryBuilder qb = QueryBuilders.boolQuery().filter(QueryBuilders.queryStringQuery(query))
                .filter(QueryBuilders.rangeQuery("json.@timestamp").gte(moniElastic.getTimeFrom()).lte(moniElastic.getTimeTo()));
        SearchRequest searchRequest = new SearchRequest();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(qb);
        searchSourceBuilder.size(500);
        searchSourceBuilder.sort("json.@timestamp", SortOrder.DESC);
        searchRequest.source(searchSourceBuilder);
        searchRequest.indices(moniElastic.getIndex());
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = searchResponse.getHits().getHits();
        Integer rows = hits.length;
        logger.info(String.format("[PF1] 本轮监控结果 ： find %s hits in %s by [%s]", rows, searchResponse.getTook().getStringRep(), StringUtils.isBlank(moniElastic.getAsid()) ? moniElastic.getEnName() : moniElastic.getAsid()));
        return searchResponse;
    }


    private SearchResponse doPF2(MoniElastic moniElastic, RestHighLevelClient client) throws IOException {
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
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
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
                String replace = message.substring(message.indexOf("{"));
                JSONObject drawInfo = JSONObject.parseObject(replace);
                JSONArray winningNumbers = drawInfo.getJSONArray("winningNumbers");
                if (StringUtils.isNotEmpty(winningNumbers)) {
                    drawInfo = (JSONObject) winningNumbers.get(0);
                }
                String winNo = drawInfo.getString("winningNumber");
                String numero = drawInfo.getString("numero");
                String gameCode = drawInfo.getString("gameCode");
                if ("TWLKENO".equals(gameCode)) {
                    gameCode = "TWK8";
                }
                if ("UUFFC11X5".equals(gameCode)) {
                    gameCode = "UUFF11X5";
                }
                if ("UUSSKENO".equals(gameCode)) {
                    gameCode = "UUKENO";
                }
                if ("TWLSSC".equals(gameCode)) {
                    gameCode = "TWBGS";
                }
                if ("JSK3".equals(gameCode) || "JSSHB".equals(gameCode)) {
                    numero = numero.substring(2).replace("-", "");
                }
                if ("HLJSSC".equals(gameCode)) {
                    numero = "1".concat(numero);
                }
                //如果未找到匹配的开奖数据则记录

                Integer count = pf1DrawCompareMapper.selectPF1DrawNumberCount(gameCode, numero, winNo);
                if (count != 1) {
                    String winNumber = pf1DrawCompareMapper.selectPF1DrawNumber(gameCode, numero);
                    if (StringUtils.isBlank(winNumber)) {
                        winNumber = "Not Found";
                    }
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
                /*if ("SSQ".equals(gameCode)) {
                    int i = winNo.lastIndexOf(",");
                    StringBuilder sb = new StringBuilder(winNo);
                    sb.replace(i, i + 1, "-");
                    winNo = sb.toString();
                }*/
                //如果未找到匹配的开奖数据则记录

                int count = pf2DrawCompareMapper.selectPF2DrawNumberCount(gameCode, numero, winNo);
                //PF2 沒有BJKL8不比對
                if (count != 1 && !gameCode.equals("BJKL8")) {
                    String winNumber = pf2DrawCompareMapper.selectPF2DrawNumber(gameCode, numero);
                    if (StringUtils.isBlank(winNumber)) {
                        winNumber = "Not Found";
                    }
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

    /**
     * 导入JOB数据
     *
     * @param jobList         JOB数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName        操作用户
     * @return 结果
     */
    @Override
    public String importJob(List<MoniElastic> jobList, Boolean isUpdateSupport, String operName) {
        if (StringUtils.isNull(jobList) || jobList.size() == 0) {
            throw new BusinessException(MessageUtils.message("import.not.empty"));
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (MoniElastic job : jobList) {
            try {
                // 验证是否存在这个job
                MoniElastic m = moniElasticMapper.selectMoniElasticById(job.getId());
                if (StringUtils.isNull(m)) {
                    job.setCreateBy(operName);
                    job.setCreateTime(new Date());
                    this.insertMoniElastic(job);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、JOB(" + job.getId() + ") " + job.getAsid() + " " + MessageUtils.message("import.success"));
                } else if (isUpdateSupport) {
                    job.setUpdateBy(operName);
                    job.setUpdateTime(new Date());
                    this.updateMoniElastic(job);
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
                logger.error(msg, e);
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

    @Override
    public int updateTemplate(String template) {
        return moniElasticMapper.updateTemplate(template);
    }

    @Override
    public JSONObject doURLElasticSearch(MoniElastic moniElastic) throws IOException {
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, dataJason(moniElastic));
            String url=Constants.PLATFORM_JY8.equals(moniElastic.getPlatform())?jy8Url:payub8Url;
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("kbn-version", "7.15.0")
                    .build();
            Response response = client.newCall(request).execute();
            String jason = response.body().string();
            JSONObject jsonObject = JSON.parseObject(jason);
            return jsonObject;
    }

    /**
     *URL Kibana的jason格式
     **/
    private String dataJason(MoniElastic moniElastic){
        JSONObject queryJson;
        Map<String, Object> dataMap = new HashMap();
        if(moniElastic.getQuery().contains(" AND ") || moniElastic.getQuery().contains(" OR ")) {
            //切割Query並組合成jason格設
            String[] queryValues = moniElastic.getQuery().split(moniElastic.getQuery().contains(" AND ") ? " AND " : " OR ");
            List filterList = new ArrayList();
            Map<String, Object> bool_ValueMap = new HashMap<>();
            for (String queryValue : queryValues) {
                Map<String, Object> filter_ValueMap = new HashMap<>();
                if(queryValue.contains(":")) {
                    String[] value = queryValue.replaceAll("\\s+", "").replaceAll("\"", "").split(":");
                    Map<String, String> matchPhrase_ValueMap = new HashMap<>();
                    Map<String, Object> should_ValueMap = new HashMap<>();
                    Map<String, Object> bool2_ValueMap = Map.of("should", Arrays.asList(should_ValueMap),"minimum_should_match", 1);
                    if(value[1].contains("*")){
                        should_ValueMap.put("query_string", Map.of("fields",Arrays.asList(value[0]),"query",value[1]));
                        filter_ValueMap.put("bool", bool2_ValueMap);
                    }else {
                        should_ValueMap.put("match_phrase", matchPhrase_ValueMap);
                        if (value[0].contains("NOT")) {
                            matchPhrase_ValueMap.put(value[0].replaceAll("NOT", ""), value[1]);
                            filter_ValueMap.put("bool", Map.of("must_not", Map.of("bool", bool2_ValueMap)));
                        } else {
                            matchPhrase_ValueMap.put(value[0], value[1]);
                            filter_ValueMap.put("bool", bool2_ValueMap);
                        }
                    }
                }else if(queryValue.contains("*")){
                    filter_ValueMap.put("query_string",Map.of("query",queryValue));
                }else{
                    filter_ValueMap.put("multi_match",Map.of("type","phrase","query",queryValue.replaceAll("\"", ""),"lenient",true));
                }
                filterList.add(filter_ValueMap);
            }

            if (moniElastic.getQuery().contains(" OR ")) {
                bool_ValueMap.put("should", filterList);
                bool_ValueMap.put("minimum_should_match", 1);
            }else{
                bool_ValueMap.put("filter", filterList);
            }
            dataMap.put("bool", bool_ValueMap);
            queryJson = new JSONObject(dataMap);
        }else if(moniElastic.getQuery().contains(":")) {
            String[] queryValues = moniElastic.getQuery().replaceAll("\\s+", "").replaceAll("\"", "").split(":");
            dataMap.put("bool",Map.of("should",Arrays.asList(Map.of("match_phrase",Map.of(queryValues[0],queryValues[1]))),"minimum_should_match",1));
            queryJson = new JSONObject(dataMap);
        }else {
            dataMap.put("multi_match",Map.of("type","phrase","query",moniElastic.getQuery().replaceAll("\"", ""),"lenient",true));
            queryJson = new JSONObject(dataMap);
        }
        //size:1 列出1筆內容
        //track_total_hits:true 顯示查出筆數
        String dataJson= String.format("""
                {"batch":[{"request":{"params":{"index":"%s",
                "body":{"sort":[{"@timestamp":{"order":"desc"}}],"fields":[{"field":"*",
                "include_unmapped":"true"}],
                "size":1,"_source":false,
                "query":{"bool":{"filter":[%s,
                {"range":{"@timestamp":{"format":"strict_date_optional_time",
                "gte":"%s",
                "lte":"%s"}}}]}},
                "highlight":{"pre_tags":["@kibana-highlighted-field@"],
                "post_tags":["@/kibana-highlighted-field@"],
                "fields":{"*":{}},"fragment_size":2147483647}
                },"track_total_hits":true}}}]}""",moniElastic.getIndex(),queryJson,moniElastic.getTimeFrom(),moniElastic.getTimeTo());
        logger.info(String.format("URL_Kibana 本轮监控json ： 格式 %s by [%s]",dataJson, StringUtils.isBlank(moniElastic.getAsid()) ? moniElastic.getEnName() : moniElastic.getAsid()));
        return  dataJson;
    }
}
