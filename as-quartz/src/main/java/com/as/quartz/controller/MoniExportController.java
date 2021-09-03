package com.as.quartz.controller;

import com.as.common.annotation.Log;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.poi.ExcelUtil;
import com.as.quartz.domain.MoniExport;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniExportService;
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
 * 自动报表任务Controller
 *
 * @author kolin
 * @date 2021-07-23
 */
@Controller
@RequestMapping("/monitor/exportJob")
public class MoniExportController extends BaseController {
    private String prefix = "monitor/exportJob";

    @Autowired
    private IMoniExportService moniExportService;

    @Autowired
    private IJobService sysJobService;

    @RequiresPermissions("monitor:exportJob:view")
    @GetMapping()
    public String exportJob() {
        return prefix + "/exportJob";
    }

    /**
     * 查询自动报表任务列表
     */
    @RequiresPermissions("monitor:exportJob:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MoniExport moniExport) {
        startPage();
        List<MoniExport> list = moniExportService.selectMoniExportList(moniExport);
        return getDataTable(list);
    }

    /**
     * 获取选择表格
     */
    @GetMapping("/getSelectTable")
    public String getSelectTable() {
        return prefix + "/selectTable";
    }

    /**
     * 导出自动报表任务列表
     */
    @RequiresPermissions("monitor:exportJob:export")
    @Log(title = "自动报表任务", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(MoniExport moniExport) {
        List<MoniExport> list = moniExportService.selectMoniExportList(moniExport);
        ExcelUtil<MoniExport> util = new ExcelUtil<MoniExport>(MoniExport.class);
        return util.exportExcel(list, "自动报表任务数据");
    }

    /**
     * 新增自动报表任务
     */
    @GetMapping("/add")
    public String add() {
        return prefix + "/add";
    }

    /**
     * 新增保存自动报表任务
     */
    @RequiresPermissions("monitor:exportJob:add")
    @Log(title = "自动报表任务", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated MoniExport moniExport) throws SchedulerException {
        if (!CronUtils.isValid(moniExport.getCronExpression())) {
            return AjaxResult.error("新增任务'" + moniExport.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniExportService.insertMoniExport(moniExport));
    }

    /**
     * 修改自动报表任务
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        MoniExport moniExport = moniExportService.selectMoniExportById(id);
        mmap.put("moniExport", moniExport);
        return prefix + "/edit";
    }

    /**
     * 修改保存自动报表任务
     */
    @RequiresPermissions("monitor:exportJob:edit")
    @Log(title = "自动报表任务", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(@Validated MoniExport moniExport) throws SchedulerException {
        if (!CronUtils.isValid(moniExport.getCronExpression())) {
            return AjaxResult.error("编辑任务'" + moniExport.getChName() + "'失败，Cron表达式不正确");
        }
        return toAjax(moniExportService.updateMoniExport(moniExport));
    }

    /**
     * 删除自动报表任务
     */
    @RequiresPermissions("monitor:exportJob:remove")
    @Log(title = "自动报表任务", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) throws SchedulerException {
        moniExportService.deleteMoniExportByIds(ids);
        return success();
    }

    /**
     * 任务调度状态修改
     */
    @Log(title = "自动报表任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:exportJob:changeStatus")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(MoniExport moniExport) throws SchedulerException {
        MoniExport newJob = moniExportService.selectMoniExportById(moniExport.getId());
        newJob.setStatus(moniExport.getStatus());
        return toAjax(moniExportService.changeStatus(newJob));
    }

    /**
     * 任务详情
     *
     * @param id
     * @param mmap
     * @return
     */
    @RequiresPermissions("monitor:exportJob:detail")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("job", moniExportService.selectMoniExportById(id));
        return prefix + "/detail";
    }

    /**
     * 任务调度立即执行一次
     */
    @Log(title = "自动报表任务", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:exportJob:runOnce")
    @PostMapping("/run")
    @ResponseBody
    public AjaxResult run(MoniExport job) throws SchedulerException {
        moniExportService.run(job);
        return success();
    }

    /**
     * sql 脚本检测
     */
    @PostMapping("/test")
    @ResponseBody
    public AjaxResult test(MoniExport job) {
        return toAjax(sysJobService.sqlTest(job.getScript(), job.getJdbc()));
    }

    /**
     * 校验cron表达式是否有效
     */
    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public boolean checkCronExpressionIsValid(MoniExport job) {
        return sysJobService.checkCronExpressionIsValid(job.getCronExpression());
    }

    /**
     * 根据Cron表达式获取任务最近 几次的执行时间
     */
    @PostMapping("/getCronSchdule")
    @ResponseBody
    public String getCronSchdule(MoniExport job) {
        return sysJobService.getCronSchdule(job.getCronExpression(), 10);
    }

    @Log(title = "自动报表任务", businessType = BusinessType.IMPORT)
    @RequiresPermissions("monitor:exportJob:import")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception {
        ExcelUtil<MoniExport> util = new ExcelUtil<MoniExport>(MoniExport.class);
        List<MoniExport> jobList = util.importExcel(file.getInputStream());
        String operName = ShiroUtils.getSysUser().getLoginName();
        String message = moniExportService.importJob(jobList, updateSupport, operName);
        return AjaxResult.success(message);
    }

    @RequiresPermissions("monitor:exportJob:import")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate() {
        ExcelUtil<MoniExport> util = new ExcelUtil<MoniExport>(MoniExport.class);
        return util.importTemplateExcel("AUTO-EXPORT");
    }

}
