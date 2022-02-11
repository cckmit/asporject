package com.as.funds.service.impl;

import com.as.common.annotation.DataSource;
import com.as.common.enums.DataSourceType;
import com.as.funds.domain.PF1SourceOfFunds;
import com.as.funds.domain.PF1SourceOfFundsSummary;
import com.as.funds.mapper.PF1SourceOfFundsMapper;
import com.as.funds.service.IPF1SourceOfFundsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * PF1资金来源Service业务层处理
 *
 * @author kolin
 * @date 2021-07-05
 */
@Service
@DataSource(value = DataSourceType.PF1_SEC)
public class PF1SourceOfFundsServiceImpl implements IPF1SourceOfFundsService {
    @Autowired
    private PF1SourceOfFundsMapper pf1SourceOfFundsMapper;

    @Override
    public List<PF1SourceOfFunds> getPF1SourceOfFound(Map<String, Object> map) {
        return pf1SourceOfFundsMapper.getPF1SourceOfFound(map);
    }

    @Override
    public List<PF1SourceOfFundsSummary> getPF1SourceOfFoundSummary(Map<String, Object> map) {
        return pf1SourceOfFundsMapper.getPF1SourceOfFoundSummary(map);
    }

}
