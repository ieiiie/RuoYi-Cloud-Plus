package com.ym.system.domain.vo;

/** 匿名入口只公开展示配置，不包含菜单、租户、权限或资源地址。 */
public record PublicAppLoginVo(String appKey, String appName, String appType, String loginTheme) {
}
