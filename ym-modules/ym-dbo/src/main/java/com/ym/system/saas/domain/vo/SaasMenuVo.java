package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasMenu;

import java.io.Serializable;

/**
 * SaaS 菜单视图。
 */
@Data
@AutoMapper(target = SaasMenu.class)
public class SaasMenuVo implements Serializable {
    private Long menuId;
    private Long appId;
    private String appName;
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
