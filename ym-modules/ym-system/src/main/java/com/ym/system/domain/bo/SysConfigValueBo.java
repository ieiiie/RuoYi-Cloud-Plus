package com.ym.system.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 租户参数值修改请求；有意不接受任何参数元数据。 */
@Data
public class SysConfigValueBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "参数值不能为空")
    private String configValue;
}
