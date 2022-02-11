package com.as.order.service.impl;

import com.as.common.annotation.DataSource;
import com.as.common.enums.DataSourceType;
import com.as.order.domain.PF1OrderNumber;
import com.as.order.mapper.PF1OrderNumberMapper;
import com.as.order.service.IPF1OrderNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * PF1订单查询Service业务层处理
 * 
 * @author kolin
 * @date 2021-07-05
 */
@Service
@DataSource(value = DataSourceType.PF1_SEC)
public class PF1OrderNumberServiceImpl implements IPF1OrderNumberService 
{
    @Autowired
    private PF1OrderNumberMapper pf1OrderNumberMapper;

    /**
     * 查询PF1订单
     * 
     * @param orderNo PF1订单号
     * @return PF1订单
     */
    @Override
    public PF1OrderNumber selectPF1OrderNumber(String orderNo)
    {
        return pf1OrderNumberMapper.selectPF1OrderNumber(orderNo);
    }

}
