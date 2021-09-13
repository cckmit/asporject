package com.as.webhook.controller;

import com.alibaba.fastjson.JSONObject;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.webhook.domain.CbObject;
import com.as.webhook.domain.PushObject;
import com.as.webhook.service.IWebhookService;
import com.as.webhook.utils.HttpRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * AS webhook Controller
 */

@RestController
@RequestMapping("/webhook")
public class ASHookController extends BaseController {

    @Autowired
    private IWebhookService webhookService;

    @RequestMapping(value = "/push", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult push(HttpServletRequest request) throws Exception {
        PushObject pushObject = (PushObject) HttpRequestUtil.commonHttpRequestParamConvert(request, new PushObject());
        Map<String, Object> result = webhookService.doPush(pushObject, request);
        return AjaxResult.success(JSONObject.toJSON(result));
    }

    @RequestMapping(value = "/cb", method = {RequestMethod.POST, RequestMethod.GET})
    public AjaxResult cb(HttpServletRequest request) {
        CbObject cbObject = (CbObject) HttpRequestUtil.commonHttpRequestParamConvert(request, new CbObject());
        Map<String, Object> result = webhookService.run(cbObject, request);
        return AjaxResult.success(JSONObject.toJSON(result));
    }
}
