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
 * ElasticSearch任务对象 moni_elastic
 *
 * @author kolin
 * @date 2021-07-28
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class MoniElastic extends BaseEntity {
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
    @Excel(name = "telegram发送群")
    private String telegramConfig;

    /**
     * 请求者
     */
    @NotBlank(message = "请求者不能为空")
    @Excel(name = "请求者")
    private String requester;

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
     * 说明
     */
    @Excel(name = "说明")
    private String descr;

    /**
     * INDEX
     */
    @NotBlank(message = "INDEX不能为空")
    @Excel(name = "INDEX")
    private String index;

    /**
     * 查询条件
     */
    @NotBlank(message = "查询条件不能为空")
    @Excel(name = "查询条件")
    private String query;

    /**
     * 开始时间
     */
    @NotBlank(message = "开始时间不能为空")
    @Excel(name = "开始时间")
    private String timeFrom;

    /**
     * 结束时间
     */
    @NotBlank(message = "结束时间不能为空")
    @Excel(name = "结束时间")
    private String timeTo;

    /**
     * 自动比对：0不比对、1等于、2不等于、3大于、4小于、5无资料、6有资料
     */
    @NotBlank(message = "自动比对不能为空")
    @Excel(name = "自动比对", readConverterExp = "0=不比对,1=等于,2=不等于,3=大于,4=小于,5=无资料,6=有资料")
    private String autoMatch;

    /**
     * 预期结果
     */
    @Excel(name = "预期结果")
    private String expectedResult;

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

    /**
     * 导出字段
     */
    @Excel(name = "导出字段")
    private String exportField;

    /**
     * 調用API
     */
    @Excel(name = "調用API")
    private String relApi;

    public Date getNextValidTime() {
        if (StringUtils.isNotEmpty(cronExpression)) {
            return CronUtils.getNextExecution(cronExpression);
        }
        return null;
    }

    /**
     * kibana路徑
     */
    @Excel(name = "kibana路徑")
    private String kibanaUrl;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("asid", getAsid())
                .append("ticketNumber", getTicketNumber())
                .append("enName", getEnName())
                .append("chName", getChName())
                .append("status", getStatus())
                .append("platform", getPlatform())
                .append("cronExpression", getCronExpression())
                .append("telegramAlert", getTelegramAlert())
                .append("telegramInfo", getTelegramInfo())
                .append("telegramConfig", getTelegramConfig())
                .append("requester", getRequester())
                .append("priority", getPriority())
                .append("actionItem", getActionItem())
                .append("descr", getDescr())
                .append("index", getIndex())
                .append("query", getQuery())
                .append("timeFrom", getTimeFrom())
                .append("timeTo", getTimeTo())
                .append("autoMatch", getAutoMatch())
                .append("expectedResult", getExpectedResult())
                .append("createBy", getCreateBy())
                .append("createTime", getCreateTime())
                .append("updateBy", getUpdateBy())
                .append("updateTime", getUpdateTime())
                .append("lastAlert", getLastAlert())
                .append("ignoreAlert", getIgnoreAlert())
                .append("exportField", getExportField())
                .append("relApi", getRelApi())
                .append("kibanaUrl", getKibanaUrl())
                .toString();
    }
}
