package com.as.sqlTemplate.domain;

import com.as.common.annotation.Excel;
import com.as.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import java.util.List;

/**
 * SQL模板对象 sql_template
 *
 * @author kolin
 * @date 2021-09-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SqlTemplate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "ID")
    private Long id;

    /**
     * ASID
     */
    @Excel(name = "ASID")
    private String asid;

    /**
     * 模板名称-英文
     */
    @Excel(name = "模板名称-英文")
    @NotBlank(message = "模板名称-英文不能为空")
    private String enName;

    /**
     * 模板名称-中文
     */
    @Excel(name = "模板名称-中文")
    @NotBlank(message = "模板名称-中文不能为空")
    private String chName;

    /**
     * 状态（0正常 1停用）
     */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    @NotBlank(message = "状态不能为空")
    private String status;

    /**
     * 平台
     */
    @Excel(name = "平台")
    @NotBlank(message = "平台不能为空")
    private String platform;

    /**
     * JDBC
     */
    @Excel(name = "JDBC")
    @NotBlank(message = "JDBC不能为空")
    private String jdbc;

    /**
     * SCRIPT
     */
    @Excel(name = "SCRIPT")
    @NotBlank(message = "SCRIPT不能为空")
    private String script;

    @Excel(name = "SqlTemplateValue")
    private List<SqlTemplateValue> values;

    /** 角色组 */
    private Long[] roleIds;
}
