package com.as.quartz.controller;

import com.as.common.annotation.Log;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.poi.ExcelUtil;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniApiService;
import com.as.quartz.util.CronUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 自动API检测任务Controller
 *
 * @author kolin
 * @date 2021-07-26
 */
@Controller
@RequestMapping("/monitor/apiJob")
public class MoniApiController extends BaseController {
    private String prefix = "monitor/apiJob";

    @Autowired
    private IMoniApiService moniApiService;

    @Autowired
    private IJobService sysJobService;

    @RequiresPermissions("monitor:apiJob:view")
    @GetMapping()
    public String apiJob() {
        return prefix + "/apiJob";
    }

    /**
     * 查询自动API检测任务列表
     */
    @RequiresPermissions("monitor:apiJob:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MoniApi moniApi) {
        startPage();
        List<MoniApi> list = moniApiService.selectMoniApiList(moniApi);
        return getDataTable(list);
    }

    /**
     * 导出自动API检测任务列表
     */
    @RequiresPermissions("monitor:apiJob:export")
    @Log(title = "自动API检测任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(MoniApi moniApi) {
        List<MoniApi> list = moniApiService.selectMoniApiList(moniApi);
        ExcelUtil<MoniApi> util = new ExcelUtil<MoniApi>(MoniApi.class);
        return util.exportExcel(list, "自动API检测任务数据");
    }

    /**
     * 新增自动API检测任务
     */
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    /**
     * 新增保存自动API检测任务
     */
    @RequiresPermissions("monitor:apiJob:add")
    @Log(title = "自动API检测任务", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated MoniApi moniApi) throws SchedulerException {
        if (!CronUtils.isValid(moniApi.getCronExpression())) {
            return AjaxResult.error("新增任务'" + moniApi.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniApiService.insertMoniApi(moniApi));
    }

    /**
     * 修改自动API检测任务
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        MoniApi moniApi = moniApiService.selectMoniApiById(id);
        mmap.put("moniApi", moniApi);
        return prefix + "/edit";
    }

    /**
     * 修改保存自动API检测任务
     */
    @RequiresPermissions("monitor:apiJob:edit")
    @Log(title = "自动API检测任务", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated MoniApi moniApi) throws SchedulerException {
        if (!CronUtils.isValid(moniApi.getCronExpression())) {
            return AjaxResult.error("编辑任务'" + moniApi.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniApiService.updateMoniApi(moniApi));
    }

    /**
     * 删除自动API检测任务
     */
    @RequiresPermissions("monitor:apiJob:remove")
    @Log(title = "自动API检测任务", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) throws SchedulerException {
        moniApiService.deleteMoniApiByIds(ids);
        return success();
    }

    @RequiresPermissions("monitor:apiJob:detail")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("name", "apiJob");
        mmap.put("job", moniApiService.selectMoniApiById(id));
        return prefix + "/detail";
    }

    /**
     * 任务调度状态修改
     */
    @Log(title = "自动API检测任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:apiJob:changeStatus")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(MoniApi moniApi) throws SchedulerException {
        MoniApi newJob = moniApiService.selectMoniApiById(moniApi.getId());
        newJob.setStatus(moniApi.getStatus());
        return toAjax(moniApiService.changeStatus(newJob));
    }

    /**
     * 任务调度告警修改
     */
    @Log(title = "自动API检测任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:apiJob:changeAlert")
    @PostMapping("/changeAlert")
    @ResponseBody
    public AjaxResult changeAlert(MoniApi moniApi) throws SchedulerException {
        return toAjax(moniApiService.changeAlert(moniApi));
    }

    /**
     * 任务调度立即执行一次
     */
    @Log(title = "自动API检测任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:apiJob:runOnce")
    @PostMapping("/run")
    @ResponseBody
    public AjaxResult run(MoniApi job) throws SchedulerException {
        moniApiService.run(job);
        return success();
    }

    /**
     * api测试
     */
    @PostMapping("/test")
    @ResponseBody
    public AjaxResult test(MoniApi job) {
        ResponseEntity<String> response = moniApiService.doUrlCheck(job);
        if (response.getStatusCodeValue() == Integer.parseInt(job.getExpectedCode())) {
            return success();
        }
        return error(response.getStatusCode().toString());
    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public boolean checkCronExpressionIsValid(MoniApi job) {
        return sysJobService.checkCronExpressionIsValid(job.getCronExpression());
    }

    /**
     * 根据Cron表达式获取任务最近 几次的执行时间
     */
    @PostMapping("/getCronSchdule")
    @ResponseBody
    public String getCronSchdule(MoniApi job) {
        return sysJobService.getCronSchdule(job.getCronExpression(), 10);
    }
}
