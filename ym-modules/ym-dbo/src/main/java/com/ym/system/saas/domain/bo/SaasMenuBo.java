package com.ym.system.saas.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.*;
import lombok.Data;
import com.ym.common.core.validate.*;
import com.ym.system.saas.domain.SaasMenu;

import java.io.Serializable;

/**
 * SaaS 菜单请求对象。
 */
@Data
@AutoMapper(target = SaasMenu.class, reverseConvertGenerate = false)
public class SaasMenuBo implements Serializable {
    @NotNull(groups = EditGroup.class)
    private Long menuId;
    @NotNull(groups = {AddGroup.class, EditGroup.class})
    private Long appId;
    private Long parentId;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String menuName;
    private Integer orderNum;
    private String path;
    private String component;
    private String queryParam;
    private String isFrame;
    private String isCache;
    @NotBlank(groups = {AddGroup.class, EditGroup.class})
    private String menuType;
    private String visible;
    private String status;
    private String perms;
    private String icon;
    private String activeMenu;
    private String ext;
    private String remark;
}
