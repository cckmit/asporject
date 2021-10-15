package com.as.sqlTemplate.service;

import java.util.List;
import java.util.Map;

import com.as.sqlTemplate.domain.SqlTemplate;

/**
 * SQL模板Service接口
 * 
 * @author kolin
 * @date 2021-09-30
 */
public interface ISqlTemplateService 
{
    /**
     * 查询SQL模板
     * 
     * @param id SQL模板ID
     * @return SQL模板
     */
    public SqlTemplate selectSqlTemplateById(Long id);

    /**
     * 查询SQL模板列表
     * 
     * @param sqlTemplate SQL模板
     * @return SQL模板集合
     */
    public List<SqlTemplate> selectSqlTemplateListByUserId(SqlTemplate sqlTemplate,Long userId);

    /**
     * 导出SQL模板列表
     *
     * @param sqlTemplate SQL模板
     * @return SQL模板集合
     */
    public List<SqlTemplate> exportSqlTemplateListByUserId(SqlTemplate sqlTemplate,Long userId);

    /**
     * 新增SQL模板
     * 
     * @param sqlTemplate SQL模板
     * @return 结果
     */
    public int insertSqlTemplate(SqlTemplate sqlTemplate);

    /**
     * 修改SQL模板
     * 
     * @param sqlTemplate SQL模板
     * @return 结果
     */
    public int updateSqlTemplate(SqlTemplate sqlTemplate);

    /**
     * 批量删除SQL模板
     * 
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteSqlTemplateByIds(String ids);

    /**
     * 删除SQL模板信息
     * 
     * @param id SQL模板ID
     * @return 结果
     */
    public int deleteSqlTemplateById(Long id);

    public int changeStatus(SqlTemplate sqlTemplate);


    Map<String, Object> query(SqlTemplate sqlTemplate, Map<String, Object> params);


    String[] queryColumns(SqlTemplate sqlTemplate, Map<String, Object> params);
}
