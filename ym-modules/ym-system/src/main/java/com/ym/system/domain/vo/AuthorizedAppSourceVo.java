package com.ym.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Set;

/** 授权应用使用的源微应用及其菜单。 */
@Data
public class AuthorizedAppSourceVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;
    private String appKey;
    private String appName;
    private String entry;
    private String initialPath;
    private Boolean alive;
    private Boolean sync;
    private List<RouterVo> menus;
    private Set<String> permissions;
}
