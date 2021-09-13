package com.as.webhook.domain;

import com.as.common.annotation.Excel;
import com.as.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class PushObject extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "ID")
    private Long id;

    /**
     * 类型
     */
    @Excel(name = "类型")
    private String type;

    /**
     * ASID
     */
    @Excel(name = "ASID")
    private String asid;

    /**
     * 标题
     */
    @Excel(name = "标题")
    private String title;

    /**
     * 请求方式
     */
    @Excel(name = "请求方式")
    private String method;

    /**
     * 说明
     */
    @Excel(name = "说明")
    private String descr;

    /**
     * 备注
     */
    @Excel(name = "备注")
    private String remark;

    /**
     * tgId
     */
    @Excel(name = "tgId")
    private String tgId;

    /**
     * 邮件地址
     */
    @Excel(name = "邮件地址")
    private String mailAdd;

    /**
     * reporter
     */
    @Excel(name = "报告人")
    private String reporter;

    /**
     * IP
     */
    @Excel(name = "IP")
    private String ip;

    /**
     * 执行状态（0成功 1失败 2错误）
     */
    @Excel(name = "执行状态", readConverterExp = "0=成功,1=失败,2=错误")
    private String status;

    /**
     * 执行信息
     */
    @Excel(name = "执行信息")
    private String message;
}
