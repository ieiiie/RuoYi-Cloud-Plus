package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 租户菜单。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SaasMenu extends BaseEntity {
    @TableId("menu_id")
    private Long menuId;
    private Long appId;
    private Long parentId;
    private String menuName;
    private Integer orderNum;
    private String path;
    private String component;
    private String queryParam;
    private String isFrame;
    private String isCache;
    private String menuType;
    private String visible;
    private String status;
    private String perms;
    private String icon;
    private String activeMenu;
    private String ext;
    private String remark;
}
