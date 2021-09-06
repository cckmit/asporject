package com.as.quartz.service.impl;

import com.as.common.constant.Constants;
import com.as.common.core.text.Convert;
import com.as.common.utils.ShiroUtils;
import com.as.quartz.domain.MoniJobLog;
import com.as.quartz.mapper.MoniJobLogMapper;
import com.as.quartz.mapper.MoniJobMapper;
import com.as.quartz.service.IMoniJobLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * SQL检测任務LOGService业务层处理
 *
 * @author kolin
 * @date 2021-07-16
 */
@Service
public class MoniJobLogServiceImpl implements IMoniJobLogService {
    @Autowired
    private MoniJobLogMapper moniJobLogMapper;

    /**
     * 查询SQL检测任務LOG
     *
     * @param id SQL检测任務LOGID
     * @return SQL检测任務LOG
     */
    @Override
    public MoniJobLog selectMoniJobLogById(Long id) {
        return moniJobLogMapper.selectMoniJobLogById(id);
    }

    /**
     * 查询SQL检测任務LOG列表
     *
     * @param moniJobLog SQL检测任務LOG
     * @return SQL检测任務LOG
     */
    @Override
    public List<MoniJobLog> selectMoniJobLogList(MoniJobLog moniJobLog) {
        return moniJobLogMapper.selectMoniJobLogList(moniJobLog);
    }

    /**
     * 查询SQL检测任務未成功LOG
     *
     * @return SQL检测任務LOG集合
     */
    @Override
    public List<MoniJobLog> selectMoniJobLogListNoSuccess() {
        return moniJobLogMapper.selectMoniJobLogListNoSuccess();
    }

    /**
     * 新增SQL检测任務LOG
     *
     * @param moniJobLog 调度日志信息
     */
    @Override
    public void addJobLog(MoniJobLog moniJobLog) {
        moniJobLogMapper.insertMoniJobLog(moniJobLog);
    }

    /**
     * 更新SQL检测任務LOG
     *
     * @param moniJobLog 调度日志信息
     */
    @Override
    public void updateJobLog(MoniJobLog moniJobLog) {
        moniJobLogMapper.updateMoniJobLog(moniJobLog);
    }

    /**
     * 删除SQL检测任務LOG对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteMoniJobLogByIds(String ids) {
        return moniJobLogMapper.deleteMoniJobLogByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除SQL检测任務LOG信息
     *
     * @param id SQL检测任務LOGID
     * @return 结果
     */
    @Override
    public int deleteMoniJobLogById(Long id) {
        return moniJobLogMapper.deleteMoniJobLogById(id);
    }

    /**
     * 清空任务日志
     */
    @Override
    public void cleanSqlJobLog() {
        moniJobLogMapper.cleanSqlJobLog();
    }


    /**
     * 回调
     *
     * @param id
     * @return
     */
    @Override
    @Transactional
    public int callback(Long id) {
        MoniJobLog moniJobLog = moniJobLogMapper.selectMoniJobLogById(id);
        moniJobLog.setAlertStatus(Constants.FAIL);
        moniJobLog.setOperator(ShiroUtils.getSysUser().getLoginName());
        return moniJobLogMapper.callbackMoniJobLog(moniJobLog);
    }

    @Override
    public List<MoniJobLog> selectLossByIds(String[] jobIds) {
        return moniJobLogMapper.selectLossByIds(jobIds);
    }
}
