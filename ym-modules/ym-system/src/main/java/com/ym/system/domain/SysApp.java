package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * SaaS 全局应用。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_app")
public class SysApp extends BaseEntity {

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
