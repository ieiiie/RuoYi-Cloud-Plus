package com.ym.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.system.domain.SysGlobalSocial;

/**
 * 全局第三方账号绑定数据层。
 */
public interface SysGlobalSocialMapper extends BaseMapperPlus<SysGlobalSocial, SysGlobalSocial> {

    /**
     * 物理删除全局第三方绑定，释放 auth_id 与 open_id 的唯一约束以支持重新绑定。
     *
     * @param id 绑定记录ID
     * @return 删除行数
     */
    @Delete("DELETE FROM sys_global_social WHERE id = #{id}")
    int deleteBindingById(@Param("id") Long id);
}
