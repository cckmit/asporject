package com.as.quartz.mapper;

import com.as.quartz.domain.MoniApi;

import java.util.List;

/**
 * 自动API检测任务Mapper接口
 *
 * @author kolin
 * @date 2021-07-26
 */
public interface MoniApiMapper {
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
    public int insertMoniApi(MoniApi moniApi);

    /**
     * 修改自动API检测任务
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    public int updateMoniApi(MoniApi moniApi);

    /**
     * 查询全部自动API检测任务
     *
     * @return
     */
    public List<MoniApi> selectMoniApiAll();

    /**
     * 删除自动API检测任务
     *
     * @param id 自动API检测任务ID
     * @return 结果
     */
    public int deleteMoniApiById(Long id);

    /**
     * 修改最后告警时间
     *
     * @param moniApi
     */
    int updateMoniApiLastAlertTime(MoniApi moniApi);

    int updateTemplate(String template);
}
