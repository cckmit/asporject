package com.as.order.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * PF1订单对象 lott_fc3d_order_main
 *
 * @author kolin
 * @date 2021-07-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PF1OrderNumber implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{2,30}$" , message = "订单号不能包含特殊字符/空格/符号")
    private String orderNo;

    /**
     * 会员账号
     */
    private String account;

    /**
     * 追号起始号
     */
    private String restarNo;

    /**
     * 彩种ID
     */
    private String sortId;

    /**
     * 总倍数
     */
    private String totalTimes;

    /**
     * 是否追号(0为不追号,1为追号，2位预选号)
     */
    private String recall;

    /**
     * 投注系数
     */
    private String kindValue;

    /**
     * 追号期数
     */
    private String recallNum;

    /**
     * 出号后放弃
     */
    private String winQuit;

    /**
     * 中奖后停止
     */
    private String winStop;

    /**
     * 系列模式值
     */
    private String seriesValue;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("orderNo" , getOrderNo())
                .append("account" , getAccount())
                .append("restarNo" , getRestarNo())
                .append("sortId" , getSortId())
                .append("totalTimes" , getTotalTimes())
                .append("recall" , getRecall())
                .append("kindValue" , getKindValue())
                .append("recallNum" , getRecallNum())
                .append("winQuit" , getWinQuit())
                .append("winStop" , getWinStop())
                .append("seriesValue" , getSeriesValue())
                .toString();
    }
}
