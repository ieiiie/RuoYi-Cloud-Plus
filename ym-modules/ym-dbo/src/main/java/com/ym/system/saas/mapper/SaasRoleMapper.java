package com.ym.system.saas.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.ym.system.saas.domain.SaasRole;

/** SaaS 租户角色 Mapper。 */
public interface SaasRoleMapper extends BaseMapper<SaasRole> {

    /**
     * 查询租户的模板角色，包含已逻辑删除记录。
     */
    @Select("SELECT * FROM sys_role WHERE tenant_id = #{tenantId} AND template_id = #{templateId} LIMIT 1")
    SaasRole selectTemplateRoleIncludingDeleted(@Param("tenantId") String tenantId,
                                                @Param("templateId") Long templateId);

    /**
     * 恢复已删除的模板角色并重新应用模板属性。
     */
    @Update("""
        UPDATE sys_role
        SET role_key = #{roleKey},
            role_sort = #{roleSort},
            data_scope = #{dataScope},
            status = #{status},
            template_version = #{templateVersion},
            is_builtin = 1,
            tenant_deletable = #{tenantDeletable},
            del_flag = '0',
            update_time = SYSDATE()
        WHERE role_id = #{roleId}
        """)
    int restoreTemplateRole(SaasRole role);
}
