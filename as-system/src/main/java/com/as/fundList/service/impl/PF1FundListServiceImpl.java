package com.as.fundList.service.impl;

import com.as.common.annotation.DataSource;
import com.as.common.enums.DataSourceType;
import com.as.fundList.domain.PF1FundList;
import com.as.fundList.mapper.PF1FundListMapper;
import com.as.fundList.service.IPF1FundListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * PF1资金对账Service业务层处理
 *
 * @author kolin
 * @date 2021-07-05
 */
@Service
@DataSource(value = DataSourceType.PF1_SEC)
public class PF1FundListServiceImpl implements IPF1FundListService {
    @Autowired
    private PF1FundListMapper pf1FundListMapper;

    @Override
    public List<PF1FundList> getPF1FundList(String account) {
        return pf1FundListMapper.getPF1FundList(account);
    }

}
