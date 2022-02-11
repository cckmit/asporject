package com.as.fundList.service.impl;

import com.as.common.annotation.DataSource;
import com.as.common.enums.DataSourceType;
import com.as.fundList.domain.PF2FundList;
import com.as.fundList.mapper.PF2FundListMapper;
import com.as.fundList.service.IPF2FundListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PF2资金对账Service业务层处理
 *
 * @author kolin
 * @date 2021-07-05
 */
@Service
@DataSource(value = DataSourceType.PF2_CORE_SEC)
public class PF2FundListServiceImpl implements IPF2FundListService {
    @Autowired
    private PF2FundListMapper pf2FundListMapper;

    @Override
    public List<PF2FundList> getPF2FundList(String account) {
        return pf2FundListMapper.getPF2FundList(account);
    }

}
