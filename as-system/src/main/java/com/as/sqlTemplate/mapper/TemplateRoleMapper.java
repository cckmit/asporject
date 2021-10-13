package com.as.sqlTemplate.mapper;

import com.as.sqlTemplate.domain.TemplateRole;
import com.as.system.domain.SysUserRole;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模板与角色关联表 数据层
 *
 * @author kolin
 */
public interface TemplateRoleMapper {
    /**
     * 通过模板ID查询模板和角色关联
     *
     * @param templateId 模板ID
     * @return 模板和角色关联列表
     */
    public List<TemplateRole> selectRoleByTemplateId(Long templateId);

    /**
     * 通过模板ID删除模板和角色关联
     *
     * @param templateId 模板ID
     * @return 结果
     */
    public int deleteRoleByTemplateId(Long templateId);


    /**
     * 批量新增模板角色信息
     *
     * @param templateRoles 模板角色列表
     * @return 结果
     */
    public int batchTemplateRole(List<TemplateRole> templateRoles);

    /**
     * 批量删除模板与角色关联
     *
     * @param templateIds 需要删除的模板数据ID
     * @return 结果
     */
        public int deleteTemplateRoleByIds(@Param("templateIds") String[] templateIds);
}
