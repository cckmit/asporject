package com.as.webhook.domain;

import com.as.common.annotation.Excel;
import com.as.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class CbObject extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * reporter
     */
    @Excel(name = "报告人")
    private String reporter;

    /**
     * JOB ID
     */
    @Excel(name = "SQL Job ID")
    private String job;

    /**
     * ELASTIC ID
     */
    @Excel(name = "Elastic Job ID")
    private String elastic;

    /**
     * API ID
     */
    @Excel(name = "Api Job ID")
    private String api;
}
