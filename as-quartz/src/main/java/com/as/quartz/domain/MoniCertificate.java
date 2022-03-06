package com.as.quartz.domain;

import com.as.common.annotation.Excel;
import com.as.common.core.domain.BaseEntity;
import com.as.common.utils.StringUtils;
import com.as.quartz.util.CronUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import java.util.Date;

/**
 * 自动API检测任务对象 moni_api
 *
 * @author kolin
 * @date 2021-07-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoniCertificate extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "ID")
    private Long id;

    /**
     * URL
     */
    @Excel(name = "Domain")
    @NotBlank(message = "Domain不能为空")
    private String Domain;


    /**
     * TICKET NUMBER
     */
    @Excel(name = "TICKET NUMBER")
    private String ticketNumber;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "證書生效時間", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date beforeTime;


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "證書失效時間", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date vaildTime;

    /**
     * 任务名称-英文
     */
    @Excel(name = "任务名称-英文")
    @NotBlank(message = "任务名称-英文不能为空")
    private String enName;

    /**
     * 任务名称-中文
     */
    @Excel(name = "任务名称-中文")
    @NotBlank(message = "任务名称-中文不能为空")
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
     * 频率
     */
    @Excel(name = "频率")
    @NotBlank(message = "频率不能为空")
    private String cronExpression;


    /**
     * 是否TELEGRAM告警（0正常 1停用）
     */
    @Excel(name = "是否TELEGRAM告警", readConverterExp = "0=正常,1=停用")
    @NotBlank(message = "是否TELEGRAM告警不能为空")
    private String telegramAlert;

    /**
     * 告警信息
     */
    @Excel(name = "告警信息")
    private String telegramInfo;

    /**
     * telegram发送群组配置
     */
    @Excel(name = "telegram发送群组配置")
    private String telegramConfig;

    /**
     * 优先级
     */
    @Excel(name = "优先级")
    @NotBlank(message = "优先级不能为空")
    private String priority;

    /**
     * 实施项目
     */
    @Excel(name = "实施项目")
    @NotBlank(message = "实施项目不能为空")
    private String actionItem;

    /**
     * 最后告警时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后告警时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date lastAlert;

    /**
     * 忽略x分钟内告警
     */
    @Excel(name = "忽略x分钟内告警")
    private Integer ignoreAlert;

    public Date getNextValidTime() {
        if (StringUtils.isNotEmpty(cronExpression)) {
            return CronUtils.getNextExecution(cronExpression);
        }
        return null;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("ticketNumber", getTicketNumber())
                .append("beforeTime",getBeforeTime())
                .append("vaildTime",getVaildTime())
                .append("enName", getEnName())
                .append("chName", getChName())
                .append("status", getStatus())
                .append("platform", getPlatform())
                .append("cronExpression", getCronExpression())
                .append("Domain", getDomain())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("telegramAlert", getTelegramAlert())
                .append("telegramInfo", getTelegramInfo())
                .append("telegramConfig", getTelegramConfig())
                .append("priority", getPriority())
                .append("actionItem", getActionItem())
                .append("lastAlert", getLastAlert())
                .append("ignoreAlert", getIgnoreAlert())
                .toString();
    }
}
