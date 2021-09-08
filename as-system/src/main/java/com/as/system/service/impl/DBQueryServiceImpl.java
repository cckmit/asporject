package com.as.system.service.impl;

import com.as.common.core.page.PageDomain;
import com.as.common.core.page.TableSupport;
import com.as.system.service.IDBQueryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DB 数据查询 服务层
 *
 * @author kolin
 */
@Service
public class DBQueryServiceImpl implements IDBQueryService {

    private JdbcTemplate jdbcTemplate;

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
     * sql 查询
     *
     * @param script
     * @param database
     * @return
     */
    @Override
    public Map<String, Object> query(String script, String database) {
        setDatasource(database);
        StringBuilder sql = processSql(script, database);
        int total = queryTotal("SELECT COUNT(1) FROM (" + script + " )  t ");
        List<Map<String, Object>> result = jdbcTemplate.queryForList(sql.toString());
        Map<String, Object> map = new HashMap<>();
        map.put("result", result);
        map.put("total", total);
        return map;
    }

    @Override
    public String[] queryColumns(String script, String database) {
        setDatasource(database);
        StringBuilder sql = reduceSql(script, database);
        SqlRowSet rows = jdbcTemplate.queryForRowSet(sql.toString());
        String[] columnNames = rows.getMetaData().getColumnNames();
        return columnNames;
    }

    private void setDatasource(String database) {
        switch (database) {
            case "mysql-as-portal":
                jdbcTemplate = new JdbcTemplate(masterDataSource);
                break;
            case "ub8-pf1":
                jdbcTemplate = new JdbcTemplate(pf1DataSource);
                break;
            case "ub8-pf1-sec":
                jdbcTemplate = new JdbcTemplate(pf1SecDataSource);
                break;
            case "ub8-pf5-core":
                jdbcTemplate = new JdbcTemplate(pf2CoreDataSource);
                break;
            case "ub8-pf5-core-sec":
                jdbcTemplate = new JdbcTemplate(pf2CoreSecDataSource);
                break;
            case "ub8-pf5-draw":
                jdbcTemplate = new JdbcTemplate(pf2DrawDataSource);
                break;
            case "data-warehouse":
                jdbcTemplate = new JdbcTemplate(pf2DwDataSource);
                break;
        }
    }

    private int queryTotal(String script) throws DataAccessException {
        Integer total = this.jdbcTemplate.queryForObject(script, Integer.class);
        return total != null ? total : 0;
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
