package com.ym.system.api.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 跨服务可序列化的角色选项。 */
@Data
public class RemoteRoleVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private Long roleId;
    private String roleName;
    private String roleKey;
    private Integer roleSort;
    private String dataScope;
    private String status;
    private String remark;
}
