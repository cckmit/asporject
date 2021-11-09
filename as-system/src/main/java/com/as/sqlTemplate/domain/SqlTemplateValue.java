package com.as.sqlTemplate.domain;

import com.as.common.annotation.Excel;
import com.as.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;

/**
 * SQL模板-value对象 sql_template_value
 *
 * @author kolin
 * @date 2021-09-30
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SqlTemplateValue extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "ID")
    private Long id;

    /**
     * 模板ID
     */
    @Excel(name = "模板ID")
    private Long templateId;

    /**
     * 参数值
     */
    @Excel(name = "参数值")
    @NotBlank(message = "参数值不能为空")
    private String templateValue;

    /**
     * value名称-英文
     */
    @Excel(name = "value名称-英文")
    @NotBlank(message = "value名称-英文不能为空")
    private String valueEnName;

    /**
     * value名称-中文
     */
    @Excel(name = "value名称-中文")
    @NotBlank(message = "value名称-中文不能为空")
    private String valueChName;

    /**
     * 提示信息-英文
     */
    @Excel(name = "提示信息-英文")
    @NotBlank(message = "提示信息-英文不能为空")
    private String enPlaceholder;

    /**
     * 提示信息-中文
     */
    @Excel(name = "提示信息-中文")
    @NotBlank(message = "提示信息-中文不能为空")
    private String chPlaceholder;

    /**
     * 显示类型
     */
    @Excel(name = "显示类型")
    @NotBlank(message = "显示类型不能为空")
    private String htmlType;

    /**
     * 比对类型
     */
    @Excel(name = "比对类型")
    @NotBlank(message = "比对类型不能为空")
    private String matchType;

    /**
     * 陣列
     */
    @Excel(name = "陣列")
    private String listValue;
}
