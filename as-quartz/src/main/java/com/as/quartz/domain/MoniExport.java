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
 * 自动报表任务对象 moni_export
 *
 * @author kolin
 * @date 2021-07-23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoniExport extends BaseEntity {
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
    @NotBlank(message = "ASID不能为空")
    private String asid;

    /**
     * TICKET NUMBER
     */
    @Excel(name = "TICKET NUMBER")
    private String ticketNumber;

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
     * JDBC
     */
    @Excel(name = "JDBC")
    @NotBlank(message = "JDBC不能为空")
    private String jdbc;

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
     * SCRIPT
     */
    @Excel(name = "SCRIPT")
    @NotBlank(message = "SCRIPT不能为空")
    private String script;

    /**
     * 收件人
     */
    @Excel(name = "收件人")
    @NotBlank(message = "收件人不能为空")
    private String mailTo;

    /**
     * 收件人副本
     */
    @Excel(name = "收件人副本")
    private String mailCc;

    /**
     * 收件人密件副本
     */
    @Excel(name = "收件人密件副本")
    private String mailBcc;

    /**
     * 邮件主题
     */
    @Excel(name = "邮件主题")
    @NotBlank(message = "邮件主题不能为空")
    private String mailSubject;

    /**
     * 邮件内容
     */
    @Excel(name = "邮件内容")
    private String mailContent;

    /**
     * 请求者
     */
    @Excel(name = "请求者")
    @NotBlank(message = "请求者不能为空")
    private String requester;

    /**
     * 最后导出时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后导出时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date lastExport;

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
                .append("asid", getAsid())
                .append("ticketNumber", getTicketNumber())
                .append("enName", getEnName())
                .append("chName", getChName())
                .append("status", getStatus())
                .append("jdbc", getJdbc())
                .append("platform", getPlatform())
                .append("cronExpression", getCronExpression())
                .append("script", getScript())
                .append("mailTo", getMailTo())
                .append("mailCc", getMailCc())
                .append("mailBcc", getMailBcc())
                .append("mailSubject", getMailSubject())
                .append("mailContent", getMailContent())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("requester", getRequester())
                .append("lastExport", getLastExport())
                .toString();
    }
}
