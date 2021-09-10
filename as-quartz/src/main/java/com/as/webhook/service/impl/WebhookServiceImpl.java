package com.as.webhook.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.as.common.constant.Constants;
import com.as.common.core.text.Convert;
import com.as.common.utils.DateUtils;
import com.as.common.utils.ExceptionUtil;
import com.as.common.utils.StringUtils;
import com.as.common.utils.spring.SpringUtils;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.domain.MoniJob;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniApiService;
import com.as.quartz.service.IMoniElasticService;
import com.as.quartz.service.IMoniJobService;
import com.as.quartz.util.Mail;
import com.as.quartz.util.ScheduleUtils;
import com.as.webhook.constant.TypeConstants;
import com.as.webhook.domain.PushObject;
import com.as.webhook.enums.ResultEnum;
import com.as.webhook.mapper.WebhookMapper;
import com.as.webhook.service.IWebhookService;
import com.as.webhook.utils.Result;
import com.pengrad.telegrambot.response.SendResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
public class WebhookServiceImpl implements IWebhookService {

    @Autowired
    private WebhookMapper webhookMapper;

    @Autowired
    private IMoniJobService jobService;

    @Autowired
    private IMoniElasticService elasticService;

    @Autowired
    private IMoniApiService apiService;

    @Override
    public Map<String, Result> doPush(PushObject pushObject, HttpServletRequest request) {
        Map<String, Result> result = new HashMap();
        boolean flag = true;
        Date date = new Date();
        pushObject.setCreateTime(date);
        pushObject.setIp(getIpAddress(request));
        String types = pushObject.getType();
        if (StringUtils.isNotEmpty(types)) {
            String[] typeArr = types.split("/");
            for (String type : typeArr) {
                if (TypeConstants.TG.equalsIgnoreCase(type)) {

                    if (StringUtils.isNotEmpty(pushObject.getTgId())) {
                        String[] tgIds = pushObject.getTgId().split(";");
                        StringBuilder tgInfo = new StringBuilder();
                        if (StringUtils.isNotEmpty(pushObject.getReporter())) {
                            tgInfo.append("\\[").append(processStr(pushObject.getReporter())).append("]");
                        }
                        tgInfo.append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, date));
                        if (StringUtils.isNotEmpty(pushObject.getTitle())) {
                            tgInfo.append("\n").append("*").append("TITLE:").append("*").append(processStr(pushObject.getTitle()));
                        }
                        if (StringUtils.isNotEmpty(pushObject.getDescr())) {
                            tgInfo.append("\n").append("*").append("DESCR:").append("*").append(processStr(pushObject.getDescr()));
                        }
                        if (StringUtils.isNotEmpty(pushObject.getRemark())) {
                            tgInfo.append("\n").append("*").append("REMARK:").append("*").append(processStr(pushObject.getRemark()));
                        }
                        for (String tgId : tgIds) {
                            try {
                                String[] tgData = ScheduleUtils.getTgData(tgId, true);
                                String bot = tgData[0];
                                String chatId = tgData[1];
                                SendResponse response = ScheduleUtils.sendMessage(bot, chatId, tgInfo.toString(), null);
                                if (response.isOk()) {
                                    result.put(type + "-" + tgId, Result.success());
                                } else {
                                    flag = false;
                                    result.put(type + "-" + tgId, Result.result(ResultEnum.TG_ERROR, response.description()));
                                }
                            } catch (Exception e) {
                                flag = false;
                                result.put(type + "-" + tgId, Result.result(ResultEnum.TG_ERROR, ExceptionUtil.getRootErrorMessage(e)));
                            }
                        }
                    } else {
                        flag = false;
                        result.put(type, Result.result(ResultEnum.TG_EMPTY));
                    }

                } else if (TypeConstants.MAIL.equalsIgnoreCase(type)) {
                    try {
                        if (StringUtils.isNotEmpty(pushObject.getMailAdd())) {
                            Mail mail = new Mail();
                            mail.setTo(pushObject.getMailAdd().split(";"));
                            String reporter = pushObject.getReporter();
                            if (StringUtils.isNotEmpty(reporter)) {
                                reporter = "[" + pushObject.getReporter() + "]";
                            } else {
                                reporter = "";
                            }
                            String title = pushObject.getTitle();
                            if (StringUtils.isEmpty(title)) {
                                title = "";
                            }
                            mail.setSubject(reporter + title);
                            StringBuilder body = new StringBuilder();
                            body.append("create_time: ").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, date));
                            if (StringUtils.isNotEmpty(pushObject.getDescr())) {
                                body.append("\n").append("descr: ").append(pushObject.getDescr());
                            }
                            if (StringUtils.isNotEmpty(pushObject.getRemark())) {
                                body.append("\n").append("remark: ").append(pushObject.getRemark());
                            }
                            mail.setContent(body.toString());
                            SpringUtils.getBean(IJobService.class).sendEmail(mail);
                            result.put(type, Result.success());
                        } else {
                            flag = false;
                            result.put(type, Result.result(ResultEnum.MAIL_EMPTY));
                        }
                    } catch (Exception e) {
                        result.put(type, Result.result(ResultEnum.MAIL_ERROR, ExceptionUtil.getRootErrorMessage(e)));
                    }
                }
            }
        }
        try {
            if (flag) {
                pushObject.setStatus(Constants.SUCCESS);
            } else {
                pushObject.setStatus(Constants.FAIL);
            }
            result.put("log", Result.success());
            if (StringUtils.isEmpty(pushObject.getType())) {
                pushObject.setType("log");
            }
            pushObject.setMessage(JSONObject.toJSONString(result));
            webhookMapper.insertWebhookRecord(pushObject);
        } catch (Exception e) {
            result.put("log", Result.result(ResultEnum.LOG_ERROR, ExceptionUtil.getRootErrorMessage(e)));
        }
        return result;
    }

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("x - forwarded - for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy - Client - IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL - Proxy - Client - IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String processStr(String str) {
        return str.replace("*", "\\*")
                .replace("_", "\\_")
                .replace("`", "\\`")
                .replace("[", "\\[");
    }

    @Override
    public Map<String, Result> run(String jobId, String elasticId, String apiId, HttpServletRequest request) {
        Map<String, Result> result = new HashMap();
        StringBuilder type = new StringBuilder();
        StringBuilder remark = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("isWebhook", true);
        boolean flag = true;
        if (StringUtils.isNotEmpty(jobId)) {
            try {
                type.append(TypeConstants.JOB).append("/");
                remark.append(TypeConstants.JOB).append("(").append(jobId).append(")").append("/");
                MoniJob moniJob = jobService.selectMoniJobById(Long.valueOf(jobId));
                if (StringUtils.isNull(moniJob)) {
                    result.put(TypeConstants.JOB, Result.result(ResultEnum.JOB_EMPTY));
                    flag = false;
                } else {
                    moniJob.setParams(params);
                    jobService.run(moniJob);
                    result.put(TypeConstants.JOB, Result.success());
                }
            } catch (Exception e) {
                result.put(TypeConstants.JOB, Result.result(ResultEnum.FAILED, ExceptionUtil.getRootErrorMessage(e)));
                flag = false;
            }
        }
        if (StringUtils.isNotEmpty(elasticId)) {
            type.append(TypeConstants.ELASTIC).append("/");
            remark.append(TypeConstants.ELASTIC).append("(").append(elasticId).append(")").append("/");
            try {
                MoniElastic moniElastic = elasticService.selectMoniElasticById(Long.valueOf(elasticId));
                if (StringUtils.isNull(moniElastic)) {
                    result.put(TypeConstants.ELASTIC, Result.result(ResultEnum.ELASTIC_EMPTY));
                    flag = false;
                } else {
                    moniElastic.setParams(params);
                    elasticService.run(moniElastic);
                    result.put(TypeConstants.ELASTIC, Result.success());
                }
            } catch (Exception e) {
                result.put(TypeConstants.ELASTIC, Result.result(ResultEnum.FAILED, ExceptionUtil.getRootErrorMessage(e)));
                flag = false;
            }
        }
        if (StringUtils.isNotEmpty(apiId)) {
            type.append(TypeConstants.API).append("/");
            remark.append(TypeConstants.API).append("(").append(apiId).append(")").append("/");
            try {
                MoniApi moniApi = apiService.selectMoniApiById(Long.valueOf(apiId));
                if (StringUtils.isNull(moniApi)) {
                    result.put(TypeConstants.API, Result.result(ResultEnum.API_EMPTY));
                    flag = false;
                } else {
                    moniApi.setParams(params);
                    apiService.run(moniApi);
                    result.put(TypeConstants.API, Result.success());
                }
            } catch (Exception e) {
                result.put(TypeConstants.API, Result.result(ResultEnum.FAILED, ExceptionUtil.getRootErrorMessage(e)));
                flag = false;
            }
        }
        if (StringUtils.isNotEmpty(type)) {
            PushObject pushObject = new PushObject();
            pushObject.setCreateTime(new Date());
            pushObject.setReporter("webhook");
            pushObject.setTitle("webhook run job >> " + remark.substring(0, remark.length() - 1));
            pushObject.setType(type.substring(0, type.length() - 1));
            pushObject.setRemark(remark.substring(0, remark.length() - 1));
            pushObject.setMessage(JSONObject.toJSONString(result));
            if (flag) {
                pushObject.setStatus(Constants.SUCCESS);
            } else {
                pushObject.setStatus(Constants.FAIL);
            }
            pushObject.setIp(getIpAddress(request));
            webhookMapper.insertWebhookRecord(pushObject);
        }
        return result;
    }

    /**
     * 查询webhook请求记录
     *
     * @param id webhook请求记录ID
     * @return webhook请求记录
     */
    @Override
    public PushObject selectWebhookRecordById(Long id) {
        return webhookMapper.selectWebhookRecordById(id);
    }

    /**
     * 查询webhook请求记录列表
     *
     * @param pushObject webhook请求记录
     * @return webhook请求记录
     */
    @Override
    public List<PushObject> selectWebhookRecordList(PushObject pushObject) {
        return webhookMapper.selectWebhookRecordList(pushObject);
    }

    /**
     * 新增webhook请求记录
     *
     * @param pushObject webhook请求记录
     * @return 结果
     */
    @Override
    public int insertWebhookRecord(PushObject pushObject) {
        pushObject.setCreateTime(DateUtils.getNowDate());
        return webhookMapper.insertWebhookRecord(pushObject);
    }

    /**
     * 修改webhook请求记录
     *
     * @param pushObject webhook请求记录
     * @return 结果
     */
    @Override
    public int updateWebhookRecord(PushObject pushObject) {
        return webhookMapper.updateWebhookRecord(pushObject);
    }

    /**
     * 删除webhook请求记录对象
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    public int deleteWebhookRecordByIds(String ids) {
        return webhookMapper.deleteWebhookRecordByIds(Convert.toStrArray(ids));
    }

    /**
     * 删除webhook请求记录信息
     *
     * @param id webhook请求记录ID
     * @return 结果
     */
    @Override
    public int deleteWebhookRecordById(Long id) {
        return webhookMapper.deleteWebhookRecordById(id);
    }
}