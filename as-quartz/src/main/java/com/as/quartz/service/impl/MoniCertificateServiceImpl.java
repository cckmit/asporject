package com.as.quartz.service.impl;

import com.as.common.core.text.Convert;
import com.as.common.utils.DateUtils;
import com.as.common.utils.ShiroUtils;
import com.as.quartz.domain.MoniCertificate;
import com.as.quartz.job.MoniApiExecution;
import com.as.quartz.mapper.MoniCertificateMapper;
import com.as.quartz.service.IMoniCertificateService;
import com.as.quartz.util.ScheduleUtils;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MoniCertificateServiceImpl implements IMoniCertificateService
{
    @Autowired
    private MoniCertificateMapper moniCertificateMapper;

    /**
     * 查询證書期限檢測
     *
     * @param id 證書期限檢測ID
     * @return 證書期限檢測
     */
    @Override
    public MoniCertificate selectMoniCertificateById(Long id)
    {
        return moniCertificateMapper.selectMoniCertificateById(id);
    }

    /**
     * 查询證書期限檢測列表
     *
     * @param moniCertificate 證書期限檢測
     * @return 證書期限檢測
     */
    @Override
    public List<MoniCertificate> selectMoniCertificateList(MoniCertificate moniCertificate)
    {
        return moniCertificateMapper.selectMoniCertificateList(moniCertificate);
    }

    /**
     * 新增證書期限檢測
     *
     * @param moniCertificate 證書期限檢測
     * @return 结果
     */
    @Override
    public int insertMoniCertificate(MoniCertificate moniCertificate) {
        moniCertificate.setCreateTime(DateUtils.getNowDate());
        moniCertificate.setCreateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniCertificateMapper.insertMoniCertificate(moniCertificate);
        //建立監控任務後加上
//        if (rows > 0) {
//            MoniApiExecution moniApiExecution = MoniApiExecution.buildJob(moniCertificate);
//            ScheduleUtils.createScheduleJob(scheduler, moniApiExecution);
//        }
        return rows;
    }
    /**
     * 修改證書期限檢測
     *
     * @param moniCertificate 證書期限檢測
     * @return 结果
     */
    @Override
    public int updateMoniCertificate(MoniCertificate moniCertificate)
    {
        return moniCertificateMapper.updateMoniCertificate(moniCertificate);
    }

    /**
     * 删除證書期限檢測对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteMoniCertificateByIds(String ids)
    {
        return moniCertificateMapper.deleteMoniCertificateByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除證書期限檢測信息
     *
     * @param id 證書期限檢測ID
     * @return 结果
     */
    @Override
    public int deleteMoniCertificateById(Long id)
    {
        return moniCertificateMapper.deleteMoniCertificateById(id);
    }
}
