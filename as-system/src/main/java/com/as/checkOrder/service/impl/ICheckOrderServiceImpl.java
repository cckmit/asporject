package com.as.checkOrder.service.impl;

import com.as.checkOrder.domain.CheckOrder;
import com.as.checkOrder.domain.OdsCheckOrder;
import com.as.checkOrder.mapper.CheckOrderMapper;
import com.as.checkOrder.service.ICheckOrderService;
import com.as.common.annotation.DataSource;
import com.as.common.enums.DataSourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 多平台订单比对Service业务层处理
 *
 * @author kolin
 * @date 2021-07-05
 */
@Service
public class ICheckOrderServiceImpl implements ICheckOrderService {
    @Autowired
    private CheckOrderMapper checkOrderMapper;


    @Override
    @DataSource(value = DataSourceType.PF1_SEC)
    public CheckOrder findAllPf1ByAccounts(String account) {
        return checkOrderMapper.findAllPf1ByAccounts(account);
    }

    @Override
    @DataSource(value = DataSourceType.PF2_CORE_SEC)
    public CheckOrder findAllPf2ByAccounts(String account) {
        return checkOrderMapper.findAllPf2ByAccounts(account);
    }

    @Override
    @DataSource(value = DataSourceType.PF2_DW)
    public List<OdsCheckOrder> findAllOdsByAccounts(String account) {
        return checkOrderMapper.findAllOdsByAccounts(account);
    }
}
