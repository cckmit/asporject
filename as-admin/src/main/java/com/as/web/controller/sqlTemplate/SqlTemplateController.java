package com.as.web.controller.sqlTemplate;

import com.as.common.annotation.Log;
import com.as.common.core.controller.BaseController;
import com.as.common.core.domain.AjaxResult;
import com.as.common.core.domain.entity.SysRole;
import com.as.common.core.domain.entity.SysUser;
import com.as.common.core.page.TableDataInfo;
import com.as.common.enums.BusinessType;
import com.as.common.utils.MessageUtils;
import com.as.common.utils.ShiroUtils;
import com.as.common.utils.poi.ExcelUtil;
import com.as.sqlTemplate.domain.SqlTemplate;
import com.as.sqlTemplate.domain.SqlTemplateValue;
import com.as.sqlTemplate.service.ISqlTemplateService;
import com.as.system.service.ISysRoleService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SQL模板Controller
 *
 * @author kolin
 * @date 2021-09-30
 */
@Controller
@RequestMapping("/as/sqlTemplate")
public class SqlTemplateController extends BaseController {
    private String prefix = "sqlTemplate";

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISqlTemplateService sqlTemplateService;

    @RequiresPermissions("as:sqlTemplate:view")
    @GetMapping()
    public String sqlTemplate(ModelMap mmap) {
        mmap.put("roles", roleService.selectRoleAll().stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return prefix + "/sqlTemplate";
    }

    /**
     * 查询SQL模板列表
     */
    @RequiresPermissions("as:sqlTemplate:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(SqlTemplate sqlTemplate) {
        startPage();
        Long userId = ShiroUtils.getUserId();
        List<SqlTemplate> list = sqlTemplateService.selectSqlTemplateListByUserId(sqlTemplate, userId);
        return getDataTable(list);
    }

    /**
     * 导出SQL模板列表
     */
    @RequiresPermissions("as:sqlTemplate:export")
    @Log(title = "SQL模板", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @ResponseBody
    public AjaxResult export(SqlTemplate sqlTemplate) {
        Long userId = ShiroUtils.getUserId();
        List<SqlTemplate> list = sqlTemplateService.selectSqlTemplateListByUserId(sqlTemplate, userId);
        ExcelUtil<SqlTemplate> util = new ExcelUtil<SqlTemplate>(SqlTemplate.class);
        return util.exportExcel(list, "SQL模板数据");
    }

    /**
     * 新增SQL模板
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        mmap.put("roles", roleService.selectRoleAll().stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return prefix + "/add";
    }

    /**
     * 新增保存SQL模板
     */
    @RequiresPermissions("as:sqlTemplate:add")
    @Log(title = "SQL模板", businessType = BusinessType.INSERT)
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult addSave(@Validated SqlTemplate sqlTemplate) {
        return toAjax(sqlTemplateService.insertSqlTemplate(sqlTemplate));
    }

    /**
     * 修改SQL模板
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        Long userId = ShiroUtils.getUserId();
        SqlTemplate sqlTemplate = sqlTemplateService.selectSqlTemplateById(id);
        List<SysRole> roles = roleService.selectRolesByTemplateId(id);
        mmap.put("sqlTemplate", sqlTemplate);
        mmap.put("roles", SysUser.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return prefix + "/edit";
    }

    /**
     * 修改保存SQL模板
     */
    @RequiresPermissions("as:sqlTemplate:edit")
    @Log(title = "SQL模板", businessType = BusinessType.UPDATE)
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(SqlTemplate sqlTemplate) {
        return toAjax(sqlTemplateService.updateSqlTemplate(sqlTemplate));
    }

    /**
     * 删除SQL模板
     */
    @RequiresPermissions("as:sqlTemplate:remove")
    @Log(title = "SQL模板", businessType = BusinessType.DELETE)
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        return toAjax(sqlTemplateService.deleteSqlTemplateByIds(ids));
    }

    /**
     * 状态修改
     */
    @Log(title = "SQL模板", businessType = BusinessType.UPDATE)
    @RequiresPermissions("monitor:sqlJob:changeStatus")
    @PostMapping("/changeStatus")
    @ResponseBody
    public AjaxResult changeStatus(SqlTemplate sqlTemplate) {
        return toAjax(sqlTemplateService.changeStatus(sqlTemplate));
    }

    /**
     * 使用SQL模板
     */
    @GetMapping("/use/{id}")
    public String use(@PathVariable("id") Long id, ModelMap mmap) {
        String locale = LocaleContextHolder.getLocale().toString();
        SqlTemplate sqlTemplate = sqlTemplateService.selectSqlTemplateById(id);
        mmap.put("sqlTemplate", sqlTemplate);
        mmap.put("locale", locale);
        return prefix + "/templateQuery";
    }

    /**
     * 检测模板状态
     */
    @GetMapping("/checkStatus/{id}")
    @ResponseBody
    public AjaxResult checkStatus(@PathVariable("id") Long id) {
        SqlTemplate sqlTemplate = sqlTemplateService.selectSqlTemplateById(id);
        if ("1".equals(sqlTemplate.getStatus())) {
            return error(MessageUtils.message("sql.template.stop"));
        }
        return success();
    }

    @PostMapping("/query")
    @ResponseBody
    public TableDataInfo query(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        requestParams.forEach((key, value) -> params.put(key.trim(), value[0]));
        SqlTemplate sqlTemplate = sqlTemplateService.selectSqlTemplateById(Long.valueOf((String) params.get("sqlTemplateId")));
        List<SqlTemplateValue> values = sqlTemplate.getValues();
        for (SqlTemplateValue value : values) {
            if ("in".equals(value.getMatchType())) {
                String a = (String) params.get(value.getTemplateValue().trim());
                String[] array = a.split(",");
                params.put(value.getTemplateValue().trim(), Arrays.asList(array));
            }
        }
        params.remove("sqlTemplateId");
        Map<String, Object> map = sqlTemplateService.query(sqlTemplate, params);
        List<Map<String, Object>> list = (List<Map<String, Object>>) map.get("result");
        int total = (int) map.get("total");
        return getDataTable(list, total);
    }

    /**
     * 动态获取列
     */
    @PostMapping("/ajaxColumns")
    @ResponseBody
    public AjaxResult ajaxColumns(HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        requestParams.forEach((key, value) -> params.put(key.trim(), value[0]));
        SqlTemplate sqlTemplate = sqlTemplateService.selectSqlTemplateById(Long.valueOf((String) params.get("sqlTemplateId")));
        List<SqlTemplateValue> values = sqlTemplate.getValues();
        for (SqlTemplateValue value : values) {
            if ("in".equals(value.getMatchType())) {
                String a = (String) params.get(value.getTemplateValue().trim());
                String[] array = a.split(",");
                params.put(value.getTemplateValue().trim(), Arrays.asList(array));
            }
        }
        params.remove("sqlTemplateId");
        String[] columnNames = sqlTemplateService.queryColumns(sqlTemplate, params);
        return AjaxResult.success(columnNames);
    }
}
