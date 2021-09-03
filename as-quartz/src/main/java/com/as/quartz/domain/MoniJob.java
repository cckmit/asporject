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
 * SQL检测任务对象 moni_job
 *
 * @author kolin
 * @date 2021-07-14
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoniJob extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * ID
     */
    @Excel(name = "ID")
    private Long id;

    /**
     * ASID
     */
    @NotBlank(message = "ASID不能为空")
    @Excel(name = "ASID")
    private String asid;

    /**
     * 任務名稱-英文
     */
    @NotBlank(message = "英文名称不能为空")
    @Excel(name = "任务名称-英文")
    private String enName;

    /**
     * 任務名稱-中文
     */
    @NotBlank(message = "中文名称不能为空")
    @Excel(name = "任务名称-中文")
    private String chName;

    /**
     * 說明
     */
    @Excel(name = "任务说明")
    private String descr;

    /**
     * 狀態：0正常、1停用
     */
    @NotBlank(message = "状态不能为空")
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /**
     * JDBC
     */
    @NotBlank(message = "JDBC不能为空")
    @Excel(name = "JDBC")
    private String jdbc;

    /**
     * 平台
     */
    @NotBlank(message = "平台不能为空")
    @Excel(name = "平台")
    private String platform;

    /**
     * 頻率
     */
    @NotBlank(message = "执行频率不能为空")
    @Excel(name = "执行频率")
    private String cronExpression;

    /**
     * SCRIPT
     */
    @NotBlank(message = "SCRIPT不能为空")
    @Excel(name = "SCRIPT")
    private String script;

    /**
     * 自動比對：0不比對、1等於、2不等於、3大於、4小於、5无资料、6有资料
     */
    @NotBlank(message = "自动比对不能为空")
    @Excel(name = "自动比对", readConverterExp = "0=不比对,1=等于,2=不等于,3=大于,4=小于,5=无资料,6=有资料")
    private String autoMatch;

    /**
     * 預期結果
     */
    @Excel(name = "预期结果")
    private String expectedResult;

    /**
     * 关联导出ID
     */
    @Excel(name = "关联导出ID")
    private String relExport;

    /**
     * 是否telegram告警：0正常、1停用
     */
    @NotBlank(message = "是否telegram告警不能为空")
    @Excel(name = "是否telegram告警", readConverterExp = "0=正常,1=停用")
    private String telegramAlert;

    /**
     * 告警信息
     */
    @Excel(name = "告警信息")
    private String telegramInfo;

    /**
     * 告警群组
     */
    @Excel(name = "告警群组")
    private String telegramConfig;

    /**
     * 请求者
     */
    @NotBlank(message = "请求者不能为空")
    @Excel(name = "请求者")
    private String requester;

    /**
     * Ticket Number
     */
    @Excel(name = "Ticket Number")
    private String ticketNumber;

    /**
     * 优先级
     */
    @NotBlank(message = "优先级不能为空")
    @Excel(name = "优先级")
    private String priority;

    /**
     * 实施项目
     */
    @NotBlank(message = "实施项目不能为空")
    @Excel(name = "实施项目")
    private String actionItem;

    /**
     * 最后告警时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "最后告警时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss", type = Excel.Type.EXPORT)
    private Date lastAlert;

    /**
     * 調用API
     */
    @Excel(name = "調用API")
    private String relApi;

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
                .append("ID", getId())
                .append("enName", getEnName())
                .append("chName", getChName())
                .append("DESCR", getDescr())
                .append("STATUS", getStatus())
                .append("JDBC", getJdbc())
                .append("PLATFORM", getPlatform())
                .append("cronExpression", getCronExpression())
                .append("SCRIPT", getScript())
                .append("autoMatch", getAutoMatch())
                .append("expectedResult", getExpectedResult())
                .append("createUser", getCreateBy())
                .append("createTime", getCreateTime())
                .append("alterUser", getUpdateBy())
                .append("alterTime", getUpdateTime())
                .append("relExport", getRelExport())
                .append("telegramAlert", getTelegramAlert())
                .append("telegramInfo", getTelegramInfo())
                .append("asid", getAsid())
                .append("requester", getRequester())
                .append("ticketNumber", getTicketNumber())
                .append("priority", getPriority())
                .append("actionItem", getActionItem())
                .append("lastAlert", getLastAlert())
                .append("ignoreAlert", getIgnoreAlert())
                .append("relApi", getRelApi())
                .toString();
    }
}
