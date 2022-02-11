package com.as.order.service.impl;

import com.as.common.annotation.DataSource;
import com.as.common.enums.DataSourceType;
import com.as.order.domain.PF2AccountInfo;
import com.as.order.domain.PF2OrderNumber;
import com.as.order.domain.PF2OrderNumberDetail;
import com.as.order.domain.PF2OrderNumberInfo;
import com.as.order.mapper.PF2OrderNumberMapper;
import com.as.order.service.IPF2OrderNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PF2订单查询Service业务层处理
 *
 * @author kolin
 * @date 2021-07-05
 */
@Service
@DataSource(value = DataSourceType.PF2_CORE_SEC)
public class PF2OrderNumberServiceImpl implements IPF2OrderNumberService {
    @Autowired
    private PF2OrderNumberMapper pf2OrderNumberMapper;

    /**
     * 查询PF2订单 db
     *
     * @param orderNo PF2订单号
     * @return PF2订单
     */
    @Override
    public PF2OrderNumber selectPF2OrderNumberDB(String orderNo) {
        return pf2OrderNumberMapper.selectPF2OrderNumberDB(orderNo);
    }

    /**
     * 查询PF2订单 rdc
     *
     * @param orderNo PF2订单号
     * @return PF2订单
     */
    @Override
    public PF2OrderNumber selectPF2OrderNumberRDC(String orderNo) {
        return pf2OrderNumberMapper.selectPF2OrderNumberRDC(orderNo);
    }

    /**
     * 查询PF2订单 详情
     *
     * @param orderNo PF2订单查询ID
     * @return PF2订单查询
     */
    @Override
    public PF2OrderNumberDetail selectPF2OrderNumberDetail(String orderNo) {
        return pf2OrderNumberMapper.selectPF2OrderNumberDetail(orderNo);
    }

    /**
     *查询单倍投注注数
     * @param orderNo
     * @return
     */
    @Override
    public String selectBetStakeByOrderNum(String orderNo){
        return pf2OrderNumberMapper.selectBetStakeByOrderNum(orderNo);
    }

    /**
     *查询投注信息
     * @param orderNo
     * @return
     */
    @Override
    public List<PF2OrderNumberInfo> selectOrderInfoByOrderNum(String orderNo) {
        return pf2OrderNumberMapper.selectOrderInfoByOrderNum(orderNo);
    }

    /**
     *查询投注会员信息
     * @param orderNo
     * @return
     */
    @Override
    public List<PF2AccountInfo> selectAccountInfoByOrderNum(String orderNo) {
        return pf2OrderNumberMapper.selectAccountInfoByOrderNum(orderNo);
    }
}
