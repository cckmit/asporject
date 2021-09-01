package com.as.quartz.service.impl;

import com.as.common.constant.DictTypeConstants;
import com.as.common.constant.ScheduleConstants;
import com.as.common.core.text.Convert;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.StringUtils;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.job.MoniApiExecution;
import com.as.quartz.mapper.MoniApiMapper;
import com.as.quartz.service.IMoniApiService;
import com.as.quartz.util.ScheduleUtils;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动API检测任务Service业务层处理
 *
 * @author kolin
 * @date 2021-07-26
 */
@Service
public class MoniApiServiceImpl implements IMoniApiService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private MoniApiMapper moniApiMapper;

    /**
     * 查询自动API检测任务
     *
     * @param id 自动API检测任务ID
     * @return 自动API检测任务
     */
    @Override
    public MoniApi selectMoniApiById(Long id) {
        return moniApiMapper.selectMoniApiById(id);
    }

    /**
     * 查询自动API检测任务列表
     *
     * @param moniApi 自动API检测任务
     * @return 自动API检测任务
     */
    @Override
    public List<MoniApi> selectMoniApiList(MoniApi moniApi) {
        Long[] jobIds = Convert.toLongArray((String) moniApi.getParams().get("ids"));
        moniApi.getParams().put("ids", jobIds);
        return moniApiMapper.selectMoniApiList(moniApi);
    }

    /**
     * 新增自动API检测任务
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMoniApi(MoniApi moniApi) throws SchedulerException {
        moniApi.setCreateTime(DateUtils.getNowDate());
        moniApi.setCreateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniApiMapper.insertMoniApi(moniApi);
        if (rows > 0) {
            MoniApiExecution moniApiExecution = MoniApiExecution.buildJob(moniApi);
            ScheduleUtils.createScheduleJob(scheduler, moniApiExecution);
        }
        return rows;
    }

    /**
     * 修改自动API检测任务
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMoniApi(MoniApi moniApi) throws SchedulerException {
        MoniApi properties = selectMoniApiById(moniApi.getId());
        moniApi.setUpdateTime(DateUtils.getNowDate());
        moniApi.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniApiMapper.updateMoniApi(moniApi);
        if (rows > 0) {
            updateSchedulerJob(moniApi, properties.getPlatform());
        }
        return rows;
    }

    /**
     * 更新任务
     *
     * @param job      任务对象
     * @param jobGroup 任务组名
     */
    private void updateSchedulerJob(MoniApi job, String jobGroup) throws SchedulerException {
        MoniApiExecution moniApiExecution = MoniApiExecution.buildJob(job);
        String jobCode = moniApiExecution.toString();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobCode, jobGroup);
        if (scheduler.checkExists(jobKey)) {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }

        ScheduleUtils.createScheduleJob(scheduler, moniApiExecution);
    }

    @Override
    public int updateMoniApiLastAlertTime(MoniApi moniApi) {
        return moniApiMapper.updateMoniApiLastAlertTime(moniApi);
    }


    /**
     * 删除自动API检测任务对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public void deleteMoniApiByIds(String ids) throws SchedulerException {
        Long[] jobIds = Convert.toLongArray(ids);
        for (Long jobId : jobIds) {
            MoniApi job = moniApiMapper.selectMoniApiById(jobId);
            deleteMoniApi(job);
        }
    }

    /**
     * 删除自动API检测任务信息
     *
     * @param moniApi 自动API检测任务
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMoniApi(MoniApi moniApi) throws SchedulerException {
        MoniApiExecution moniApiExecution = MoniApiExecution.buildJob(moniApi);
        String jobCode = moniApiExecution.toString();
        String jobGroup = moniApi.getPlatform();
        int rows = moniApiMapper.deleteMoniApiById(moniApi.getId());
        if (rows > 0) {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobCode, jobGroup));
        }
        return rows;
    }

    /**
     * SQL检测任务状态修改
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int changeStatus(MoniApi job) throws SchedulerException {
        int rows = 0;
        String status = job.getStatus();
        job.setUpdateTime(DateUtils.getNowDate());
        job.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        if (ScheduleConstants.Status.NORMAL.getValue().equals(status)) {
            rows = resumeJob(job);
        } else if (ScheduleConstants.Status.PAUSE.getValue().equals(status)) {
            rows = pauseJob(job);
        }
        return rows;
    }

    /**
     * SQL检测任务告警状态修改
     *
     * @param moniApi 调度信息
     */
    @Override
    @Transactional
    public int changeAlert(MoniApi moniApi) throws SchedulerException {
        moniApi.setUpdateTime(DateUtils.getNowDate());
        moniApi.setUpdateBy(ShiroUtils.getSysUser().getLoginName());
        int rows = moniApiMapper.updateMoniApi(moniApi);
        if (rows > 0) {
            MoniApi properties = selectMoniApiById(moniApi.getId());
            updateSchedulerJob(properties, properties.getPlatform());
        }
        return rows;
    }

    /**
     * 暂停任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int pauseJob(MoniApi job) throws SchedulerException {
        MoniApiExecution moniApiExecution = new MoniApiExecution();
        moniApiExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniApiExecution.toString();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = moniApiMapper.updateMoniApi(job);
        if (rows > 0) {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobCode, job.getPlatform()));
        }
        return rows;
    }

    /**
     * 恢复任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public int resumeJob(MoniApi job) throws SchedulerException {
        MoniApiExecution moniApiExecution = new MoniApiExecution();
        moniApiExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniApiExecution.toString();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = moniApiMapper.updateMoniApi(job);
        if (rows > 0) {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobCode, job.getPlatform()));
        }
        return rows;
    }

    /**
     * 立即运行任务
     *
     * @param job 调度信息
     */
    @Override
    @Transactional
    public void run(MoniApi job) throws SchedulerException {
        MoniApiExecution moniApiExecution = new MoniApiExecution();
        moniApiExecution.setId(String.valueOf(job.getId()));
        String jobCode = moniApiExecution.toString();
        MoniApi tmpObj = selectMoniApiById(job.getId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        try {
            dataMap.put("operator", ShiroUtils.getLoginName());
        } catch (Exception e) {
            //时ShiroUtils.getLoginName()会异常，此处吞掉异常继续执行調用API
        }
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, tmpObj);
        scheduler.triggerJob(ScheduleUtils.getJobKey(jobCode, tmpObj.getPlatform()), dataMap);
    }

    /**
     * HTTP模拟请求
     *
     * @param job
     * @return
     */
    @Override
    public ResponseEntity<String> doUrlCheck(MoniApi job) {
        String url = job.getUrl();
        HttpMethod method = HttpMethod.resolve(DictUtils.getDictLabel(DictTypeConstants.API_JOB_METHOD, job.getMethod()));
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        Map<String, String> hashMap = new HashMap<>();
        //处理Body
        String bodies = job.getBody();
        if (StringUtils.isNotEmpty(bodies)) {
            String[] bodyArray = bodies.split("\r\n");
            for (String param : bodyArray) {
                String[] value = param.split(":");
                multiValueMap.add(value[0], value[1]);
                hashMap.put(value[0], value[1]);
            }
        }
        //处理Header
        HttpHeaders headerMap = new HttpHeaders();
        String headers = job.getHeader();
        if (StringUtils.isNotEmpty(headers)) {
            String[] headerArray = headers.split("\r\n");
            for (String param : headerArray) {
                String[] value = param.split(":");
                if (value.length > 1) {
                    headerMap.add(value[0], value[1]);
                } else {
                    headerMap.add(value[0], "");
                }

            }
        }

        if (HttpMethod.GET.equals(method) && StringUtils.isNotEmpty(hashMap)) {
            StringBuilder sb = new StringBuilder();
            sb.append(url).append("?");
            for (Map.Entry<String, String> entry : hashMap.entrySet()) {
                sb.append(entry.getKey()).append("=").append("{").append(entry.getKey()).append("}").append("&");
            }
            url = sb.substring(0, sb.length() - 1);
        }

        RestTemplate restTemplate = new RestTemplate();

        //content-type
        MediaType mediaType = null;
        if (StringUtils.isNotEmpty(job.getContentType())) {
            mediaType = MediaType.parseMediaType(DictUtils.getDictLabel(DictTypeConstants.API_JOB_CONTENT, job.getContentType()));
            headerMap.setContentType(mediaType);
        }


        assert method != null;
        HttpEntity<Object> request;
        ResponseEntity<String> response;
        if (!(MediaType.APPLICATION_JSON_UTF8.equals(mediaType) || MediaType.APPLICATION_JSON.equals(mediaType)) && HttpMethod.POST.equals(method)) {
            request = new HttpEntity<>(multiValueMap, headerMap);
            response = restTemplate.exchange(url, method, request, String.class);
        } else {
            if (HttpMethod.POST.equals(method)) {
                request = new HttpEntity<>(hashMap, headerMap);
                response = restTemplate.exchange(url, method, request, String.class);
            } else {
                request = new HttpEntity<>(null, headerMap);
                response = restTemplate.exchange(url, method, request, String.class, hashMap);
            }
        }

        return response;
    }

    /**
     * 調用Api
     *
     * @param relApi
     */
    @Override
    @Transactional
    public void doApi(String relApi) throws Exception {
        if (StringUtils.isNotEmpty(relApi)) {
            String[] ids = relApi.split(",");
            for (String id : ids) {
                MoniApi moniApi = selectMoniApiById(Long.parseLong(id));
                if (StringUtils.isNotNull(moniApi)) {
                    run(moniApi);
                } else {
                    throw new Exception("The related api job does not exist");
                }

            }
        }
    }
}
