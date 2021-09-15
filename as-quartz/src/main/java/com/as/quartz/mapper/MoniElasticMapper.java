package com.as.quartz.mapper;

import com.as.quartz.domain.MoniElastic;

import java.util.List;

/**
 * ElasticSearch任务Mapper接口
 *
 * @author kolin
 * @date 2021-07-28
 */
public interface MoniElasticMapper {
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
    public int insertMoniElastic(MoniElastic moniElastic);

    /**
     * 修改ElasticSearch任务
     *
     * @param moniElastic ElasticSearch任务
     * @return 结果
     */
    public int updateMoniElastic(MoniElastic moniElastic);

    /**
     * 查询全部ElasticSearch任务
     *
     * @return
     */
    public List<MoniElastic> selectMoniElasticAll();

    /**
     * 删除ElasticSearch任务
     *
     * @param id ElasticSearch任务ID
     * @return 结果
     */
    public int deleteMoniElasticById(Long id);

    /**
     * 修改最后告警时间
     *
     * @param moniElastic
     */
    int updateMoniElasticLastAlertTime(MoniElastic moniElastic);

    int updateTemplate(String template);
}
