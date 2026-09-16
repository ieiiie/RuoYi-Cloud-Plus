package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 组合应用菜单关系。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_composite_app_menu")
public class SaasCompositeAppMenu {
    private Long appId;
    private Long menuId;
}
