package com.as.webhook.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.as.common.constant.Constants;
import com.as.common.constant.DictTypeConstants;
import com.as.common.core.text.Convert;
import com.as.common.utils.DateUtils;
import com.as.common.utils.DictUtils;
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
import com.as.webhook.domain.CbObject;
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
    public Map<String, Object> doPush(PushObject pushObject, HttpServletRequest request) {
        Map<String, Object> result = new HashMap();
        String reporter = pushObject.getReporter();
        if (StringUtils.isEmpty(reporter) || !checkReporterIsExist(reporter)) {
            result.put(TypeConstants.ERROR, Result.result(ResultEnum.USER_EMPTY));
            return result;
        }
        boolean flag = true;
        Date date = new Date();
        pushObject.setCreateTime(date);
        pushObject.setIp(getIpAddress(request));
        pushObject.setReporter(reporter.toLowerCase());
        pushObject.setMethod(DictUtils.getDictLabel(DictTypeConstants.API_JOB_METHOD, request.getMethod().toUpperCase()));
        //先储存log，获取record id
        webhookMapper.insertWebhookRecord(pushObject);
        String types = pushObject.getType();
        if (StringUtils.isNotEmpty(types)) {
            String[] typeArr = types.split("/");
            for (String type : typeArr) {
                if (TypeConstants.TG.equalsIgnoreCase(type)) {

                    if (StringUtils.isNotEmpty(pushObject.getTgId())) {
                        String tgInfo = DictUtils.getDictRemark(DictTypeConstants.JOB_PUSH_TEMPLATE, Constants.DESCR_TEMPLATE_WEBHOOK);
                        String[] tgIds = pushObject.getTgId().split(";");
                        if (StringUtils.isNotEmpty(tgInfo)) {
                            tgInfo = getInfo(pushObject, date, tgInfo);
                        } else {
                            tgInfo = "*Webhook push template is not set*";
                        }
                        for (String tgId : tgIds) {
                            try {
                                String[] tgData = ScheduleUtils.getTgData(tgId, true);
                                String bot = tgData[0];
                                String chatId = tgData[1];
                                SendResponse response = ScheduleUtils.sendMessageForWebhook(bot, chatId, tgInfo);
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
                            String title = pushObject.getTitle();
                            if (StringUtils.isEmpty(title)) {
                                title = "";
                            }
                            mail.setSubject("[webhook] " + title);
                            String mailInfo = DictUtils.getDictRemark(DictTypeConstants.JOB_PUSH_TEMPLATE, Constants.MAIL_TEMPLATE_WEBHOOK);
                            if (StringUtils.isNotEmpty(mailInfo)) {
                                mailInfo = getInfo(pushObject, date, mailInfo);
                                mail.setTemplate(mailInfo);
                            } else {
                                StringBuilder body = new StringBuilder();
                                body.append("[webhook] ").append(title);
                                body.append("\n").append("CreateTime: ").append(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, date)).append(" (").append(reporter.toLowerCase()).append(")");
                                if (StringUtils.isNotEmpty(pushObject.getDescr())) {
                                    body.append("\n").append("Descr: ").append(pushObject.getDescr());
                                }
                                if (StringUtils.isNotEmpty(pushObject.getRemark())) {
                                    body.append("\n").append("Remark: ").append(pushObject.getRemark());
                                }
                                mail.setContent(body.toString());
                            }

                            SpringUtils.getBean(IJobService.class).sendEmail(mail);
                            result.put(type, Result.success());
                        } else {
                            flag = false;
                            result.put(type, Result.result(ResultEnum.MAIL_EMPTY));
                        }
                    } catch (Exception e) {
                        result.put(type, Result.result(ResultEnum.MAIL_ERROR, StringUtils.isEmpty(ExceptionUtil.getRootErrorMessage(e)) || "null".equals(ExceptionUtil.getRootErrorMessage(e)) ? e.getMessage() : ExceptionUtil.getRootErrorMessage(e)));
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
            result.put("recordId", pushObject.getId());
            pushObject.setMessage(JSONObject.toJSONString(result));
            webhookMapper.updateWebhookRecord(pushObject);
        } catch (Exception e) {
            result.put("log", Result.result(ResultEnum.LOG_ERROR, ExceptionUtil.getRootErrorMessage(e)));
        }
        return result;
    }

    private String getInfo(PushObject pushObject, Date date, String info) {
        return info.replace("{type}", StringUtils.isNotEmpty(pushObject.getType()) ? pushObject.getType() : "log")
                .replace("{title}", StringUtils.isNotEmpty(pushObject.getTitle()) ? ScheduleUtils.processStr(pushObject.getTitle()) : "")
                .replace("{time}", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, date))
                .replace("{reporter}", pushObject.getReporter())
                .replace("{descr}", StringUtils.isNotEmpty(pushObject.getDescr()) ? ScheduleUtils.processStr(pushObject.getDescr()) : "")
                .replace("{remark}", StringUtils.isNotEmpty(pushObject.getRemark()) ? ScheduleUtils.processStr(pushObject.getRemark()) : "");
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

    @Override
    public Map<String, Object> run(CbObject cbObject, HttpServletRequest request) {
        Map<String, Object> result = new HashMap();
        if (StringUtils.isEmpty(cbObject.getReporter()) || !checkReporterIsExist(cbObject.getReporter())) {
            result.put(TypeConstants.ERROR, Result.result(ResultEnum.USER_EMPTY));
            return result;
        }
        StringBuilder type = new StringBuilder();
        StringBuilder remark = new StringBuilder();
        StringBuilder asid = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        params.put("isWebhook", true);
        params.put("operator", cbObject.getReporter());
        boolean flag = true;
        if (StringUtils.isNotEmpty(cbObject.getJob())) {
            try {
                type.append(TypeConstants.JOB).append("/");
                remark.append(TypeConstants.JOB).append("(").append(cbObject.getJob()).append(")").append("/");
                MoniJob moniJob = jobService.selectMoniJobById(Long.valueOf(cbObject.getJob()));
                if (StringUtils.isNull(moniJob)) {
                    result.put(TypeConstants.JOB, Result.result(ResultEnum.JOB_EMPTY));
                    flag = false;
                } else {
                    asid.append(moniJob.getAsid()).append("/");
                    moniJob.setParams(params);
                    jobService.run(moniJob);
                    result.put(TypeConstants.JOB, Result.success());
                }
            } catch (Exception e) {
                result.put(TypeConstants.JOB, Result.result(ResultEnum.FAILED, ExceptionUtil.getRootErrorMessage(e)));
                flag = false;
            }
        }
        if (StringUtils.isNotEmpty(cbObject.getElastic())) {
            type.append(TypeConstants.ELASTIC).append("/");
            remark.append(TypeConstants.ELASTIC).append("(").append(cbObject.getElastic()).append(")").append("/");
            try {
                MoniElastic moniElastic = elasticService.selectMoniElasticById(Long.valueOf(cbObject.getElastic()));
                if (StringUtils.isNull(moniElastic)) {
                    result.put(TypeConstants.ELASTIC, Result.result(ResultEnum.ELASTIC_EMPTY));
                    flag = false;
                } else {
                    asid.append(moniElastic.getAsid()).append("/");
                    moniElastic.setParams(params);
                    elasticService.run(moniElastic);
                    result.put(TypeConstants.ELASTIC, Result.success());
                }
            } catch (Exception e) {
                result.put(TypeConstants.ELASTIC, Result.result(ResultEnum.FAILED, ExceptionUtil.getRootErrorMessage(e)));
                flag = false;
            }
        }
        if (StringUtils.isNotEmpty(cbObject.getApi())) {
            type.append(TypeConstants.API).append("/");
            remark.append(TypeConstants.API).append("(").append(cbObject.getApi()).append(")").append("/");
            try {
                MoniApi moniApi = apiService.selectMoniApiById(Long.valueOf(cbObject.getApi()));
                if (StringUtils.isNull(moniApi)) {
                    result.put(TypeConstants.API, Result.result(ResultEnum.API_EMPTY));
                    flag = false;
                } else {
                    asid.append(moniApi.getAsid()).append("/");
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
            pushObject.setReporter(cbObject.getReporter().toLowerCase());
            pushObject.setMethod(DictUtils.getDictLabel(DictTypeConstants.API_JOB_METHOD, request.getMethod().toUpperCase()));
            pushObject.setTitle("webhook run job >> " + remark.substring(0, remark.length() - 1));
            pushObject.setType(type.substring(0, type.length() - 1));
            if (StringUtils.isNotEmpty(remark)) {
                pushObject.setRemark(remark.substring(0, remark.length() - 1));
            }
            if (StringUtils.isNotEmpty(asid)) {
                pushObject.setAsid(asid.substring(0, asid.length() - 1));
            }
            //先插入获取recordId
            webhookMapper.insertWebhookRecord(pushObject);
            result.put("recordId", pushObject.getId());
            pushObject.setMessage(JSONObject.toJSONString(result));
            if (flag) {
                pushObject.setStatus(Constants.SUCCESS);
            } else {
                pushObject.setStatus(Constants.FAIL);
            }
            pushObject.setIp(getIpAddress(request));
            webhookMapper.updateWebhookRecord(pushObject);
        } else {
            result.put(TypeConstants.ERROR, Result.error());
        }
        return result;
    }

    public boolean checkReporterIsExist(String reporter) {
        int row = webhookMapper.checkReporterIsExist(reporter.toLowerCase());
        return row == 1;
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
        Long[] jobIds = Convert.toLongArray((String) pushObject.getParams().get("ids"));
        pushObject.getParams().put("ids", jobIds);
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

    @Override
    public int updateTgTemplate(String template) {
        return webhookMapper.updateTgTemplate(template);
    }

    @Override
    public int updateMailTemplate(String template) {
        return webhookMapper.updateMailTemplate(template);
    }
}