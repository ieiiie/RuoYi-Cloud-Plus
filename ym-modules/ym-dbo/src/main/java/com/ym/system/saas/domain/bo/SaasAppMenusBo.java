package com.ym.system.saas.domain.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 仅更新组合应用菜单，空列表表示清空停用应用的配置。 */
@Data
public class SaasAppMenusBo implements Serializable {
    @NotNull(message = "菜单列表不能为空，请用空数组清空配置")
    private List<@NotNull Long> menuIds;
}
