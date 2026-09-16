package com.ym.system.saas.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 应用目录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_app")
public class SaasApp extends BaseEntity {
    @TableId("app_id")
    private Long appId;
    private String appKey;
    private String appName;
    private String appType;
    /** 独立入口使用的登录页面：agriculture 或 iot。 */
    private String loginTheme;
    private String entry;
    private String initialPath;
    private Boolean alive;
    private Boolean sync;
    private String icon;
    private Integer orderNum;
    private String status;
    @TableLogic
    private String delFlag;
    private String remark;
}
