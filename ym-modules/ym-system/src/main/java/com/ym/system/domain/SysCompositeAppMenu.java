package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 组合应用菜单关联。 */
@Data
@TableName("sys_composite_app_menu")
public class SysCompositeAppMenu {
    private Long appId;
    private Long menuId;
}
