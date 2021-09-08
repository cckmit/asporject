package com.as.order.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.io.Serializable;

/**
 * PF2订单对象
 *
 * @author kolin
 * @date 2021-07-05
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PF2OrderNumber implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 订单号
     */
    @NotBlank(message = "订单号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9]{2,22}$", message = "订单号不能包含特殊字符/空格/符号")
    private String orderNo;

    /**
     * 投注时间
     */
    private String createTime;

    /**
     * 投注IP
     */
    private String clientIp;

    /**
     * 投注模式
     */
    private String betMode;

    /**
     * 系列模式
     */
    private String series;

    /**
     * 彩种类别
     */
    private String code;

    /**
     * 投注期号
     */
    private String numero;

    /**
     * 总追号数
     */
    private String totalChasePhases;

    /**
     * 出号后放弃
     */
    private String abandoning;

    /**
     * 中奖后停止
     */
    private String winningStop;

    /**
     * 系列模式值
     */
    private String multiple;

}
