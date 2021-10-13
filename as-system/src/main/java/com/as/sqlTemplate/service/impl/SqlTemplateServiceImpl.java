package com.as.sqlTemplate.service.impl;

import com.as.common.core.domain.entity.SysUser;
import com.as.common.core.page.PageDomain;
import com.as.common.core.page.TableSupport;
import com.as.common.core.text.Convert;
import com.as.common.utils.DateUtils;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.StringUtils;
import com.as.sqlTemplate.domain.SqlTemplate;
import com.as.sqlTemplate.domain.SqlTemplateValue;
import com.as.sqlTemplate.domain.TemplateRole;
import com.as.sqlTemplate.mapper.SqlTemplateMapper;
import com.as.sqlTemplate.mapper.TemplateRoleMapper;
import com.as.sqlTemplate.service.ISqlTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL模板Service业务层处理
 *
 * @author kolin
 * @date 2021-09-30
 */
@Service
public class SqlTemplateServiceImpl implements ISqlTemplateService {

    @Autowired
    private SqlTemplateMapper sqlTemplateMapper;

    @Autowired
    private TemplateRoleMapper templateRoleMapper;

    private NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("masterDataSource")
    DataSource masterDataSource;

    @Autowired
    @Qualifier("pf1DataSource")
    DataSource pf1DataSource;

    @Autowired
    @Qualifier("pf1SecDataSource")
    DataSource pf1SecDataSource;

    @Autowired
    @Qualifier("pf2CoreDataSource")
    DataSource pf2CoreDataSource;

    @Autowired
    @Qualifier("pf2CoreSecDataSource")
    DataSource pf2CoreSecDataSource;

    @Autowired
    @Qualifier("pf2DrawDataSource")
    DataSource pf2DrawDataSource;

    @Autowired
    @Qualifier("pf2DwDataSource")
    DataSource pf2DwDataSource;

    /**
     * 查询SQL模板
     *
     * @param id SQL模板ID
     * @return SQL模板
     */
    @Override
    public SqlTemplate selectSqlTemplateById(Long id) {
        return sqlTemplateMapper.selectSqlTemplateById(id);
    }

    /**
     * 查询SQL模板列表
     *
     * @param sqlTemplate SQL模板
     * @return SQL模板
     */
    @Override
    public List<SqlTemplate> selectSqlTemplateListByUserId(SqlTemplate sqlTemplate, Long userId) {

        List<SqlTemplate> sqlTemplateList = null;
        if (SysUser.isAdmin(userId)) {
            sqlTemplateList = sqlTemplateMapper.selectSqlTemplateList(sqlTemplate);
        } else {
            sqlTemplate.getParams().put("userId", userId);
            sqlTemplateList = sqlTemplateMapper.selectSqlTemplateListByUserId(sqlTemplate);
        }
        return sqlTemplateList;
    }

    /**
     * 新增SQL模板
     *
     * @param sqlTemplate SQL模板
     * @return 结果
     */
    @Override
    @Transactional
    public int insertSqlTemplate(SqlTemplate sqlTemplate) {
        sqlTemplate.setCreateTime(DateUtils.getNowDate());
        sqlTemplate.setCreateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = sqlTemplateMapper.insertSqlTemplate(sqlTemplate);
        //新增模板自定义参数
        List<SqlTemplateValue> sqlTemplateValues = sqlTemplate.getValues();
        if (StringUtils.isNotNull(sqlTemplateValues)) {
            for (SqlTemplateValue sqlTemplateValue : sqlTemplateValues) {
                sqlTemplateValue.setTemplateId(sqlTemplate.getId());
            }
            sqlTemplateMapper.batchTemplateValue(sqlTemplateValues);
        }
        // 新增模板与角色管理
        insertTemplateRole(sqlTemplate.getId(), sqlTemplate.getRoleIds());
        return rows;
    }

    /**
     * 新增模板角色信息
     *
     * @param templateId
     * @param roleIds
     */
    public void insertTemplateRole(Long templateId, Long[] roleIds) {
        if (StringUtils.isNotNull(roleIds)) {
            // 新增模板与角色管理
            List<TemplateRole> list = new ArrayList<TemplateRole>();
            for (Long roleId : roleIds) {
                TemplateRole templateRole = new TemplateRole();
                templateRole.setTemplateId(templateId);
                templateRole.setRoleId(roleId);
                list.add(templateRole);
            }
            if (list.size() > 0) {
                templateRoleMapper.batchTemplateRole(list);
            }
        }
    }

    /**
     * 修改SQL模板
     *
     * @param sqlTemplate SQL模板
     * @return 结果
     */
    @Override
    @Transactional
    public int updateSqlTemplate(SqlTemplate sqlTemplate) {
        //删除模板与角色关联
        templateRoleMapper.deleteRoleByTemplateId(sqlTemplate.getId());
        //删除模板自定义参数
        sqlTemplateMapper.deleteSqlTemplateValuesByTemplateId(sqlTemplate.getId());
        //新增模板与角色管理
        insertTemplateRole(sqlTemplate.getId(), sqlTemplate.getRoleIds());
        //新增模板自定义参数
        List<SqlTemplateValue> sqlTemplateValues = sqlTemplate.getValues();
        if (StringUtils.isNotNull(sqlTemplateValues)) {
            for (SqlTemplateValue sqlTemplateValue : sqlTemplateValues) {
                sqlTemplateValue.setTemplateId(sqlTemplate.getId());
            }
            sqlTemplateMapper.batchTemplateValue(sqlTemplateValues);
        }
        sqlTemplate.setUpdateTime(DateUtils.getNowDate());
        sqlTemplate.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        return sqlTemplateMapper.updateSqlTemplate(sqlTemplate);
    }

    /**
     * 删除SQL模板对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteSqlTemplateByIds(String ids) {
        //删除模板与角色关联
        templateRoleMapper.deleteTemplateRoleByIds(Convert.toStrArray(ids));
        //删除模板自定义参数
        sqlTemplateMapper.deleteSqlTemplateValuesByTemplateIds(Convert.toStrArray(ids));
        return sqlTemplateMapper.deleteSqlTemplateByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除SQL模板信息
     *
     * @param id SQL模板ID
     * @return 结果
     */
    @Override
    public int deleteSqlTemplateById(Long id) {
        return sqlTemplateMapper.deleteSqlTemplateById(id);
    }

    @Override
    @Transactional
    public int changeStatus(SqlTemplate sqlTemplate) {
        sqlTemplate.setUpdateTime(DateUtils.getNowDate());
        sqlTemplate.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        return sqlTemplateMapper.updateSqlTemplate(sqlTemplate);
    }

    @Override
    public Map<String, Object> query(SqlTemplate sqlTemplate, Map<String, Object> params) {
        setDatasource(sqlTemplate.getJdbc());
        StringBuilder sql = processSql(sqlTemplate.getScript(), sqlTemplate.getJdbc());
        int total = queryTotal("SELECT COUNT(1) FROM (" + sqlTemplate.getScript() + " )  t ", params);
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql.toString(), params);
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        map.put("total", total);
        return map;
    }

    private int queryTotal(String script, Map<String, Object> params) throws DataAccessException {
        Integer total = this.jdbcTemplate.queryForObject(script, params, Integer.class);
        return total != null ? total : 0;
    }

    @Override
    public String[] queryColumns(SqlTemplate sqlTemplate, Map<String, Object> params) {
        setDatasource(sqlTemplate.getJdbc());
        StringBuilder sql = reduceSql(sqlTemplate.getScript(), sqlTemplate.getJdbc());
        SqlRowSet rows = jdbcTemplate.queryForRowSet(sql.toString(), params);
        String[] columnNames = rows.getMetaData().getColumnNames();
        return columnNames;
    }

    private void setDatasource(String database) {
        switch (database) {
            case "mysql-as-portal":
                jdbcTemplate = new NamedParameterJdbcTemplate(masterDataSource);
                break;
            case "ub8-pf1":
                jdbcTemplate = new NamedParameterJdbcTemplate(pf1DataSource);
                break;
            case "ub8-pf1-sec":
                jdbcTemplate = new NamedParameterJdbcTemplate(pf1SecDataSource);
                break;
            case "ub8-pf5-core":
                jdbcTemplate = new NamedParameterJdbcTemplate(pf2CoreDataSource);
                break;
            case "ub8-pf5-core-sec":
                jdbcTemplate = new NamedParameterJdbcTemplate(pf2CoreSecDataSource);
                break;
            case "ub8-pf5-draw":
                jdbcTemplate = new NamedParameterJdbcTemplate(pf2DrawDataSource);
                break;
            case "data-warehouse":
                jdbcTemplate = new NamedParameterJdbcTemplate(pf2DwDataSource);
                break;
        }
    }

    private StringBuilder processSql(String script, String datasource) {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        StringBuilder sql = new StringBuilder();
        if (datasource.toUpperCase().contains("MYSQL")) {
            sql.append("SELECT * FROM ( ");
            sql.append(script);
            sql.append(" ) TMP_PAGE");
            if (pageNum != null && pageSize != null) {
                sql.append(" LIMIT ").append((pageNum - 1) * pageSize + "," + pageSize);
            }
        } else if (datasource.toUpperCase().contains("POSTGRES")) {
            sql.append("SELECT * FROM ( ");
            sql.append(script);
            sql.append(" ) TMP_PAGE");
            if (pageNum != null && pageSize != null) {
                sql.append(" LIMIT ").append(pageSize).append(" OFFSET ").append((pageNum - 1) * pageSize);
            }
        } else {
            sql.append("SELECT * FROM ( ");
            sql.append("SELECT ROWNUM RN,TMP_PAGE.* FROM ( ");
            sql.append(script);
            sql.append(" ) TMP_PAGE");
            if (pageNum != null && pageSize != null) {
                sql.append(" WHERE ROWNUM <= ");
                sql.append(pageNum * pageSize);
                sql.append(" ) WHERE RN > ");
                sql.append((pageNum - 1) * pageSize);
            } else {
                sql.append(")");
            }
        }

        return sql;
    }

    private StringBuilder reduceSql(String script, String datasource) {
        StringBuilder sql = new StringBuilder();
        if (datasource.toUpperCase().contains("MYSQL")) {
            sql.append("SELECT * FROM ( ");
            sql.append(script);
            sql.append(" ) TMP_PAGE LIMIT 1");
        } else if (datasource.toUpperCase().contains("POSTGRES")) {
            sql.append("SELECT * FROM ( ");
            sql.append(script);
            sql.append(" ) TMP_PAGE LIMIT 1");
        } else {
            sql.append("SELECT ROWNUM RN,TMP_PAGE.* FROM ( ");
            sql.append(script);
            sql.append(" ) TMP_PAGE WHERE ROWNUM  < 1");
        }

        return sql;
    }
}
