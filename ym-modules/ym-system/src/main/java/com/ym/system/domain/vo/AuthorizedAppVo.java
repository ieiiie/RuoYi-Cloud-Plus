package com.ym.system.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 当前用户可启动的微应用或组合应用。 */
@Data
public class AuthorizedAppVo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;
    private String appKey;
    private String appName;
    private String appType;
    private String icon;
    private Integer orderNum;
    private List<AuthorizedAppSourceVo> sources;
}
