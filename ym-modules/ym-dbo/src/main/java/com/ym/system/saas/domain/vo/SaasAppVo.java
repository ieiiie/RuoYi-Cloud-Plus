package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasApp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SaaS 应用视图。
 */
@Data
@AutoMapper(target = SaasApp.class)
public class SaasAppVo implements Serializable {
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
    private String remark;
    /** 组合应用选择的目录、页面和按钮菜单。 */
    private List<Long> menuIds;
    private LocalDateTime createTime;
}
