package com.ym.system.domain.bo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 角色排序请求。
 */
@Data
public class SysRoleSortBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotEmpty(message = "角色排序不能为空")
    private List<Long> roleIds;
}
