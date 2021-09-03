package com.as.quartz.controller;

import com.as.common.annotation.Log;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.poi.ExcelUtil;
import com.as.quartz.domain.MoniJob;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniJobService;
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
 * SQL检测任务Controller
 *
 * @author kolin
 * @date 2021-07-14
 */
@Controller
@RequestMapping("/monitor/sqlJob")
public class MoniJobController extends BaseController {
    private String prefix = "monitor/sqlJob";

    @Autowired
    private IMoniJobService moniJobService;

    @Autowired
    private IJobService sysJobService;

    @RequiresPermissions("monitor:sqlJob:view")
    @GetMapping()
    public String sqlJob() {
        return prefix + "/sqlJob";
    }

    /**
     * 查询SQL检测任务列表
     */
    @RequiresPermissions("monitor:sqlJob:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MoniJob moniJob) {
        startPage();
        List<MoniJob> list = moniJobService.selectMoniJobList(moniJob);
        return getDataTable(list);
    }

    /**
     * 导出SQL检测任务列表
     */
    @RequiresPermissions("monitor:sqlJob:export")
    @Log(title = "SQL检测任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(MoniJob moniJob) {
        List<MoniJob> list = moniJobService.selectMoniJobList(moniJob);
        ExcelUtil<MoniJob> util = new ExcelUtil<MoniJob>(MoniJob.class);
        return util.exportExcel(list, "SQL检测任务数据");
    }

    /**
     * 新增SQL检测任务
     */
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    /**
     * 新增保存SQL检测任务
     */
    @RequiresPermissions("monitor:sqlJob:add")
    @Log(title = "SQL检测任务", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated MoniJob moniJob) throws SchedulerException {
        if (!CronUtils.isValid(moniJob.getCronExpression())) {
            return AjaxResult.error("新增任务'" + moniJob.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniJobService.insertMoniJob(moniJob));
    }

    /**
     * 修改SQL检测任务
     */
    @GetMapping("/edit/{ID}")
    public String edit(@PathVariable("ID") Long ID, ModelMap mmap) {
        MoniJob moniJob = moniJobService.selectMoniJobById(ID);
        mmap.put("moniJob", moniJob);
        return prefix + "/edit";
    }

    /**
     * 修改保存SQL检测任务
     */
    @RequiresPermissions("monitor:sqlJob:edit")
    @Log(title = "SQL检测任务", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated MoniJob moniJob) throws SchedulerException {
        if (!CronUtils.isValid(moniJob.getCronExpression())) {
            return AjaxResult.error("编辑任务'" + moniJob.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniJobService.updateMoniJob(moniJob));
    }

    /**
     * 删除SQL检测任务
     */
    @RequiresPermissions("monitor:sqlJob:remove")
    @Log(title = "SQL检测任务", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) throws SchedulerException {
        moniJobService.deleteMoniJobByIds(ids);
        return success();
    }

    @RequiresPermissions("monitor:sqlJob:detail")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("name", "sqlJob");
        mmap.put("job", moniJobService.selectMoniJobById(id));
        return prefix + "/detail";
    }

    /**
     * 任务调度状态修改
     */
    @Log(title = "SQL检测任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:sqlJob:changeStatus")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(MoniJob moniJob) throws SchedulerException {
        MoniJob newJob = moniJobService.selectMoniJobById(moniJob.getId());
        newJob.setStatus(moniJob.getStatus());
        return toAjax(moniJobService.changeStatus(newJob));
    }

    /**
     * 任务调度告警修改
     */
    @Log(title = "SQL检测任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:sqlJob:changeAlert")
    @PostMapping("/changeAlert")
    @ResponseBody
    public AjaxResult changeAlert(MoniJob moniJob) throws SchedulerException {
        return toAjax(moniJobService.changeAlert(moniJob));
    }

    /**
     * 任务调度立即执行一次
     */
    @Log(title = "SQL检测任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:sqlJob:runOnce")
    @PostMapping("/run")
    @ResponseBody
    public AjaxResult run(MoniJob job) throws SchedulerException {
        moniJobService.run(job);
        return success();
    }

    /**
     * sql 脚本检测
     */
    @PostMapping("/test")
    @ResponseBody
    public AjaxResult test(MoniJob job) {
        return toAjax(sysJobService.sqlTest(job.getScript(), job.getJdbc()));
    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public boolean checkCronExpressionIsValid(MoniJob job) {
        return sysJobService.checkCronExpressionIsValid(job.getCronExpression());
    }

    /**
     * 根据Cron表达式获取任务最近 几次的执行时间
     */
    @PostMapping("/getCronSchdule")
    @ResponseBody
    public String getCronSchdule(MoniJob job) {
        return sysJobService.getCronSchdule(job.getCronExpression(), 10);
    }

    @Log(title = "SQL检测任务", businessType = BusinessType.IMPORT)
    @RequiresPermissions("monitor:sqlJob:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<MoniJob> util = new ExcelUtil<MoniJob>(MoniJob.class);
        List<MoniJob> jobList = util.importExcel(file.getInputStream());
        String operName = ShiroUtils.getSysUser().getLoginName();
        String message = moniJobService.importJob(jobList, updateSupport, operName);
        return AjaxResult.success(message);
    }

    @RequiresPermissions("monitor:sqlJob:import")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate() {
        ExcelUtil<MoniJob> util = new ExcelUtil<MoniJob>(MoniJob.class);
        return util.importTemplateExcel("SQL-Detect");
    }
}
