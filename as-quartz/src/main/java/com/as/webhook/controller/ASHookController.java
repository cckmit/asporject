package com.as.webhook.controller;

import com.alibaba.fastjson.JSONObject;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.webhook.domain.PushObject;
import com.as.webhook.service.IWebhookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/push")
    public AjaxResult push(@RequestBody PushObject pushObject, HttpServletRequest request) throws Exception {
        Map<String, Object> result = webhookService.doPush(pushObject, request);
        return AjaxResult.success(JSONObject.toJSON(result));
    }

    @GetMapping("/cb")
    public AjaxResult cb(HttpServletRequest request, @RequestParam(value = "job", required = false) String jobId, @RequestParam(value = "elastic", required = false) String elasticId, @RequestParam(value = "api", required = false) String apiId, @RequestParam(value = "reporter", required = false) String reporter) {
        Map<String, Object> result = webhookService.run(jobId, elasticId, apiId, reporter, request);
        return AjaxResult.success(JSONObject.toJSON(result));
    }
}
