package com.as.quartz.controller;

import com.as.common.annotation.Log;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.quartz.domain.MoniApi;
import com.as.quartz.domain.MoniCertificate;
import com.as.quartz.service.IJobService;
import com.as.quartz.service.IMoniCertificateService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.jetbrains.annotations.TestOnly;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/monitor/certificateJob")
public class MoniCertificateController extends BaseController {

    private String prefix = "monitor/certificateJob";

    @Autowired
    private IMoniCertificateService moniCertificateService;

    @Autowired
    private IJobService sysJobService;

    @RequiresPermissions("monitor:certificateJob:view")
    @GetMapping()
    public String certificateJob()
    {
        return prefix + "/certificateJob";
    }




    /**
     * 查询證書期限檢測列表
     */
    @RequiresPermissions("monitor:certificateJob:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MoniCertificate moniCertificate)
    {
        startPage();
        List<MoniCertificate> list = moniCertificateService.selectMoniCertificateList(moniCertificate);
        return getDataTable(list);
    }

    /**
     * 导出證書期限檢測列表
     */
//    @RequiresPermissions("system:certificate:export")
//    @Log(title = "證書期限檢測", businessType = BusinessType.EXPORT)
//    @PostMapping("/export")
//    @ResponseBody
//    public AjaxResult export(MoniCertificate moniCertificate)
//    {
//        List<MoniCertificate> list = moniCertificateService.selectMoniCertificateList(moniCertificate);
//        ExcelUtil<MoniCertificate> util = new ExcelUtil<MoniCertificate>(MoniCertificate.class);
//        return util.exportExcel(list, "證書期限檢測数据");
//    }

    /**
     * 新增證書期限檢測
     */
    @GetMapping("/add")
    public String add()
    {
        return prefix + "/add";
    }


    @GetMapping("/create")
    public String create()
    {
        return prefix + "/create";
    }

    /**
     * 新增保存證書期限檢測
     */
    @RequiresPermissions("monitor:certificateJob:add")
    @Log(title = "證書期限檢測", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(MoniCertificate moniCertificate)
    {
        return toAjax(moniCertificateService.insertMoniCertificate(moniCertificate));
    }

    /**
     * 修改證書期限檢測
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap)
    {
        MoniCertificate moniCertificate = moniCertificateService.selectMoniCertificateById(id);
        mmap.put("moniCertificate", moniCertificate);
        return prefix + "/edit";
    }

    /**
     * 修改保存證書期限檢測
     */
    @RequiresPermissions("monitor:certificateJob:edit")
    @Log(title = "證書期限檢測", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MoniCertificate moniCertificate)
    {
        return toAjax(moniCertificateService.updateMoniCertificate(moniCertificate));
    }

    /**
     * 删除證書期限檢測
     */
    @RequiresPermissions("monitor:certificateJob:remove")
    @Log(title = "證書期限檢測", businessType = BusinessType.DELETE)
    @PostMapping( "/remove")
    @ResponseBody
    public AjaxResult remove(String ids)
    {
        return toAjax(moniCertificateService.deleteMoniCertificateByIds(ids));
    }

    @PostMapping("/checkCronExpressionIsValid")
    @ResponseBody
    public boolean checkCronExpressionIsValid(MoniApi job) {
        return sysJobService.checkCronExpressionIsValid(job.getCronExpression());
    }

    @PostMapping("/getCronSchdule")
    @ResponseBody
    public String getCronSchdule(MoniApi job) {
        return sysJobService.getCronSchdule(job.getCronExpression(), 10);
    }
}