package com.as.webhook.mapper;

import com.as.webhook.domain.PushObject;

import java.util.List;

public interface WebhookMapper {


    /**
     * 新增webhook请求记录
     *
     * @param pushObject webhook请求记录
     * @return 结果
     */
    public int insertWebhookRecord(PushObject pushObject);

    /**
     * 修改webhook请求记录
     *
     * @param pushObject webhook请求记录
     * @return 结果
     */
    public int updateWebhookRecord(PushObject pushObject);

    /**
     * 查询webhook请求记录
     *
     * @param id webhook请求记录ID
     * @return webhook请求记录
     */
    public PushObject selectWebhookRecordById(Long id);

    /**
     * 查询webhook请求记录列表
     *
     * @param pushObject webhook请求记录
     * @return webhook请求记录集合
     */
    public List<PushObject> selectWebhookRecordList(PushObject pushObject);


    /**
     * 删除webhook请求记录
     *
     * @param id webhook请求记录ID
     * @return 结果
     */
    public int deleteWebhookRecordById(Long id);

    /**
     * 批量删除webhook请求记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteWebhookRecordByIds(String[] ids);

}
