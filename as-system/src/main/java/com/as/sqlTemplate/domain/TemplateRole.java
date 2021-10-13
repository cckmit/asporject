package com.as.sqlTemplate.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 模板和角色关联 SYS_ROLE_TEMPLATE
 *
 * @author kolin
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TemplateRole {
    /**
     * 模板ID
     */
    private Long templateId;

    /**
     * 角色ID
     */
    private Long roleId;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("templateId" , getTemplateId())
                .append("roleId" , getRoleId())
                .toString();
    }
}
