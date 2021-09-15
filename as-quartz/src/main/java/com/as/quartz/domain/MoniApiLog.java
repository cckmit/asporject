package com.as.quartz.domain;

import com.as.common.annotation.Excel;
import com.as.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * 自动API检测任务LOG对象 moni_api_log
 *
 * @author kolin
 * @date 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoniApiLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "ID")
    private Long id;

    /**
     * 任务ID
     */
    @Excel(name = "任务ID")
    @NotBlank(message = "任务ID不能为空")
    private Long apiId;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "开始时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "结束时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;

    /**
     * 预期状态码
     */
    @Excel(name = "预期状态码")
    private String expectedCode;

    /**
     * 执行结果
     */
    @Excel(name = "执行结果")
    private String executeResult;

    /**
     * 异常信息
     */
    @Excel(name = "异常信息")
    private String exceptionLog;

    /**
     * 执行时长(秒)
     */
    @Excel(name = "执行时长(秒)")
    private Long executeTime;

    /**
     * 执行状态（0成功 1失败 2错误）
     */
    @Excel(name = "执行状态", readConverterExp = "0=成功,1=失败,2=错误")
    private String status;

    /**
     * 告警（0正常 1停用）
     */
    @Excel(name = "告警", readConverterExp = "0=正常,1=停用")
    private String alertStatus;

    /**
     * 操作者,系统则为system
     */
    @Excel(name = "操作者,系统则为system")
    private String operator;

    /**
     * 是否告警
     */
    @Excel(name = "是否告警")
    private String isAlert;

    /**
     * 响应内容
     */
    @Excel(name = "响应内容")
    private String response;

    private MoniApi moniApi;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("apiId", getApiId())
                .append("startTime", getStartTime())
                .append("endTime", getEndTime())
                .append("expectedCode", getExpectedCode())
                .append("executeResult", getExecuteResult())
                .append("exceptionLog", getExceptionLog())
                .append("executeTime", getExecuteTime())
                .append("status", getStatus())
                .append("alertStatus", getAlertStatus())
                .append("operator", getOperator())
                .toString();
    }
}
