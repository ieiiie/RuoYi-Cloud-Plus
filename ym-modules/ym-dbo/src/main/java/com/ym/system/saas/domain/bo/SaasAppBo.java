package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasApp;

import java.io.Serializable;
import java.util.List;

/**
 * SaaS 应用请求对象。
 */
@Data
@AutoMapper(target = SaasApp.class, reverseConvertGenerate = false)
public class SaasAppBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long appId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "应用标识只能包含小写字母、数字和连字符")
    private String appKey;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String appName;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
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
}
