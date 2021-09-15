package com.as.quartz.mapper;

import com.as.quartz.domain.MoniJob;
import org.quartz.SchedulerException;

import java.util.List;

/**
 * SQL检测任務Mapper接口
 *
 * @author kolin
 * @date 2021-07-16
 */
public interface MoniJobMapper {
    /**
     * 查询SQL检测任务
     *
     * @param ID SQL检测任务ID
     * @return SQL检测任务
     */
    public MoniJob selectMoniJobById(Long ID);

    /**
     * 查询SQL检测任务列表
     *
     * @param moniJob SQL检测任务
     * @return SQL检测任务集合
     */
    public List<MoniJob> selectMoniJobList(MoniJob moniJob);

    /**
     * 新增SQL检测任务
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    public int insertMoniJob(MoniJob moniJob) throws SchedulerException;

    /**
     * 修改SQL检测任务
     *
     * @param moniJob SQL检测任务
     * @return 结果
     */
    public int updateMoniJob(MoniJob moniJob) throws SchedulerException;

    /**
     * 查询全部SQL检测任务
     * @return
     */
    public List<MoniJob> selectMoniJobAll();

    /**
     * 根据id删除SQL检测任务
     * @param id
     * @return
     */
    public int deleteMoniJobById(Long id);

    /**
     * 修改最后告警时间
     * @param moniJob
     */
    int updateMoniJobLastAlertTime(MoniJob moniJob);

    int updateTemplate(String template);
}
