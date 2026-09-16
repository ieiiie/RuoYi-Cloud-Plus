package com.ym.system.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 角色用户授权变更请求。
 */
@Data
public class SysRoleAuthUserBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotNull(message = "新增授权用户不能为空")
    private Long[] addUserIds;

    @NotNull(message = "取消授权用户不能为空")
    private Long[] removeUserIds;
}
