package com.as.sqlTemplate.mapper;

import com.as.sqlTemplate.domain.SqlTemplate;
import com.as.sqlTemplate.domain.SqlTemplateValue;

import java.util.List;

/**
 * SQL模板Mapper接口
 *
 * @author kolin
 * @date 2021-09-30
 */
public interface SqlTemplateMapper {
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
    public List<SqlTemplate> selectSqlTemplateList(SqlTemplate sqlTemplate);

    /**
     * 根据用户ID查询SQL模板列表
     *
     * @param sqlTemplate SQL模板
     * @return SQL模板集合
     */
    public List<SqlTemplate> selectSqlTemplateListByUserId(SqlTemplate sqlTemplate);

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
     * 删除SQL模板
     *
     * @param id SQL模板ID
     * @return 结果
     */
    public int deleteSqlTemplateById(Long id);

    /**
     * 批量删除SQL模板
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteSqlTemplateByIds(String[] ids);

    public int batchTemplateValue(List<SqlTemplateValue> sqlTemplateValues);

    public int deleteSqlTemplateValuesByTemplateId(Long templateId);

    public int deleteSqlTemplateValuesByTemplateIds(String[] templateIds);
}
