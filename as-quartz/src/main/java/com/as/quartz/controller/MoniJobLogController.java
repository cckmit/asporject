package com.as.quartz.controller;

import com.as.common.annotation.Log;
import com.as.common.constant.DictTypeConstants;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.DictUtils;
import com.as.common.utils.StringUtils;
import com.as.common.utils.poi.ExcelUtil;
import com.as.quartz.domain.MoniJob;
import com.as.quartz.domain.MoniJobLog;
import com.as.quartz.service.IMoniJobLogService;
import com.as.quartz.service.IMoniJobService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SQL检测任務LOGController
 *
 * @author kolin
 * @date 2021-07-16
 */
@Controller
@RequestMapping("/monitor/sqlJobLog")
public class MoniJobLogController extends BaseController {
    private String prefix = "monitor/sqlJob";

    @Autowired
    private IMoniJobLogService moniJobLogService;

    @Autowired
    private IMoniJobService moniJobService;

    @RequiresPermissions("monitor:sqlJobLog:view")
    @GetMapping()
    public String sqlJob(@RequestParam(value = "jobId", required = false) Long jobId, ModelMap mmap) {
        if (StringUtils.isNotNull(jobId)) {
            MoniJob job = moniJobService.selectMoniJobById(jobId);
            mmap.put("job", job);
        }
        return prefix + "/sqlJobLog";
    }

    /**
     * 查询SQL检测任務LOG列表
     */
    @RequiresPermissions("monitor:sqlJobLog:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MoniJobLog moniJobLog) {
        startPage();
        List<MoniJobLog> list = moniJobLogService.selectMoniJobLogList(moniJobLog);
        return getDataTable(list);
    }

    /**
     * 导出SQL检测任務LOG列表
     */
    @RequiresPermissions("monitor:sqlJobLog:export")
    @Log(title = "SQL检测任務LOG", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(MoniJobLog moniJobLog) {
        List<MoniJobLog> list = moniJobLogService.selectMoniJobLogList(moniJobLog);
        ExcelUtil<MoniJobLog> util = new ExcelUtil<MoniJobLog>(MoniJobLog.class);
        return util.exportExcel(list, "SQL检测任務LOG数据");
    }

    /**
     * 删除SQL检测任務LOG
     */
    @RequiresPermissions("monitor:sqlJobLog:remove")
    @Log(title = "SQL检测任務LOG", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(moniJobLogService.deleteMoniJobLogByIds(ids));
    }

    /**
     * 日志详情
     *
     * @param id
     * @param mmap
     * @return
     */
    @RequiresPermissions("monitor:sqlJobLog:detail")
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("name", "sqlJobLog");
        MoniJobLog moniJobLog = moniJobLogService.selectMoniJobLogById(id);
        String descr = moniJobLog.getMoniJob().getDescr();
        if (StringUtils.isNotEmpty(descr)) {
            descr = descr.replace("{id}", String.valueOf(moniJobLog.getJobId()))
                    .replace("{asid}", moniJobLog.getMoniJob().getAsid())
                    .replace("{zh_name}", moniJobLog.getMoniJob().getChName())
                    .replace("{en_name}", moniJobLog.getMoniJob().getEnName())
                    .replace("{platform}", DictUtils.getDictLabel(DictTypeConstants.UB8_PLATFORM_TYPE, moniJobLog.getMoniJob().getPlatform()));
        }
        moniJobLog.getMoniJob().setDescr(descr);
        mmap.put("jobLog", moniJobLog);
        return prefix + "/detail";
    }

    /**
     * 清空日志
     *
     * @return
     */
    @Log(title = "SQL检测任務LOG", businessType = BusinessType.CLEAN)
    @RequiresPermissions("monitor:sqlJobLog:clear")
    @PostMapping("/clean")
    @ResponseBody
    public AjaxResult clean() {
        moniJobLogService.cleanSqlJobLog();
        return success();
    }

    /**
     * 回调
     *
     * @return
     */
    @Log(title = "SQL检测任務LOG", businessType = BusinessType.UPDATE)
    @GetMapping("/callback/{id}")
    @ResponseBody
    public AjaxResult callback(@PathVariable("id") Long id) {
        return toAjax(moniJobLogService.callback(id));
    }
}
