package com.as.quartz.service;


import com.as.quartz.domain.MoniCertificate;

import java.util.List;

public interface IMoniCertificateService
{
    /**
     * 查询證書期限檢測
     *
     * @param id 證書期限檢測ID
     * @return 證書期限檢測
     */
    public MoniCertificate selectMoniCertificateById(Long id);

    /**
     * 查询證書期限檢測列表
     *
     * @param moniCertificate 證書期限檢測
     * @return 證書期限檢測集合
     */
    public List<MoniCertificate> selectMoniCertificateList(MoniCertificate moniCertificate);

    /**
     * 新增證書期限檢測
     *
     * @param moniCertificate 證書期限檢測
     * @return 结果
     */
    public int insertMoniCertificate(MoniCertificate moniCertificate);

    /**
     * 修改證書期限檢測
     *
     * @param moniCertificate 證書期限檢測
     * @return 结果
     */
    public int updateMoniCertificate(MoniCertificate moniCertificate);

    /**
     * 批量删除證書期限檢測
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMoniCertificateByIds(String ids);

    /**
     * 删除證書期限檢測信息
     *
     * @param id 證書期限檢測ID
     * @return 结果
     */
    public int deleteMoniCertificateById(Long id);
}