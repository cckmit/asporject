package com.as.quartz.controller;

import com.as.common.annotation.Log;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.poi.ExcelUtil;
import com.as.quartz.domain.MoniElastic;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniElasticService;
import com.as.quartz.util.CronUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ElasticSearch任务Controller
 *
 * @author kolin
 * @date 2021-07-28
 */
@Controller
@RequestMapping("/monitor/elasticJob")
public class MoniElasticController extends BaseController {
    private String prefix = "monitor/elasticJob";

    @Autowired
    private IMoniElasticService moniElasticService;

    @Autowired
    private IJobService sysJobService;

    @RequiresPermissions("monitor:elasticJob:view")
    @GetMapping()
    public String elasticJob() {
        return prefix + "/elasticJob";
    }

    /**
     * 查询ElasticSearch任务列表
     */
    @RequiresPermissions("monitor:elasticJob:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MoniElastic moniElastic) {
        startPage();
        List<MoniElastic> list = moniElasticService.selectMoniElasticList(moniElastic);
        return getDataTable(list);
    }

    /**
     * 导出ElasticSearch任务列表
     */
    @RequiresPermissions("monitor:elasticJob:export")
    @Log(title = "ElasticSearch任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(MoniElastic moniElastic) {
        List<MoniElastic> list = moniElasticService.selectMoniElasticList(moniElastic);
        ExcelUtil<MoniElastic> util = new ExcelUtil<MoniElastic>(MoniElastic.class);
        return util.exportExcel(list, "ElasticSearch任务数据");
    }

    /**
     * 新增ElasticSearch任务
     */
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    /**
     * 新增保存ElasticSearch任务
     */
    @RequiresPermissions("monitor:elasticJob:add")
    @Log(title = "ElasticSearch任务", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated MoniElastic moniElastic) throws SchedulerException {
        if (!CronUtils.isValid(moniElastic.getCronExpression())) {
            return AjaxResult.error("新增任务'" + moniElastic.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniElasticService.insertMoniElastic(moniElastic));
    }

    /**
     * 修改ElasticSearch任务
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        MoniElastic moniElastic = moniElasticService.selectMoniElasticById(id);
        mmap.put("moniElastic", moniElastic);
        return prefix + "/edit";
    }

    /**
     * 修改保存ElasticSearch任务
     */
    @RequiresPermissions("monitor:elasticJob:edit")
    @Log(title = "ElasticSearch任务", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated MoniElastic moniElastic) throws SchedulerException {
        if (!CronUtils.isValid(moniElastic.getCronExpression())) {
            return AjaxResult.error("编辑任务'" + moniElastic.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniElasticService.updateMoniElastic(moniElastic));
    }

    /**
     * 删除ElasticSearch任务
     */
    @RequiresPermissions("monitor:elasticJob:remove")
    @Log(title = "ElasticSearch任务", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) throws SchedulerException {
        moniElasticService.deleteMoniElasticByIds(ids);
        return success();
    }

    @RequiresPermissions("monitor:elasticJob:detail")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("job", moniElasticService.selectMoniElasticById(id));
        return prefix + "/detail";
    }

    /**
     * 任务调度状态修改
     */
    @Log(title = "ElasticSearch任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:elasticJob:changeStatus")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(MoniElastic moniElastic) throws SchedulerException {
        MoniElastic newJob = moniElasticService.selectMoniElasticById(moniElastic.getId());
        newJob.setStatus(moniElastic.getStatus());
        return toAjax(moniElasticService.changeStatus(newJob));
    }

    /**
     * 任务调度告警修改
     */
    @Log(title = "ElasticSearch任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:elasticJob:changeAlert")
    @PostMapping("/changeAlert")
    @ResponseBody
    public AjaxResult changeAlert(MoniElastic moniElastic) throws SchedulerException {
        return toAjax(moniElasticService.changeAlert(moniElastic));
    }

    /**
     * 任务调度立即执行一次
     */
    @Log(title = "ElasticSearch任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:elasticJob:runOnce")
    @PostMapping("/run")
    @ResponseBody
    public AjaxResult run(MoniElastic job) throws SchedulerException {
        moniElasticService.run(job);
        return success();
    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public boolean checkCronExpressionIsValid(MoniElastic job) {
        return sysJobService.checkCronExpressionIsValid(job.getCronExpression());
    }

    /**
     * 根据Cron表达式获取任务最近 几次的执行时间
     */
    @PostMapping("/getCronSchdule")
    @ResponseBody
    public String getCronSchdule(MoniElastic job) {
        return sysJobService.getCronSchdule(job.getCronExpression(), 10);
    }

    @Log(title = "ElasticSearch任务", businessType = BusinessType.IMPORT)
    @RequiresPermissions("monitor:elasticJob:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<MoniElastic> util = new ExcelUtil<MoniElastic>(MoniElastic.class);
        List<MoniElastic> jobList = util.importExcel(file.getInputStream());
        String operName = ShiroUtils.getSysUser().getLoginName();
        String message = moniElasticService.importJob(jobList, updateSupport, operName);
        return AjaxResult.success(message);
    }

    @RequiresPermissions("monitor:exportJob:import")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate() {
        ExcelUtil<MoniElastic> util = new ExcelUtil<MoniElastic>(MoniElastic.class);
        return util.importTemplateExcel("ELASTIC-JOB");
    }
}
