package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasTenantPackage;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * SaaS 套餐视图。
 */
@Data
@AutoMapper(target = SaasTenantPackage.class)
public class SaasTenantPackageVo implements Serializable {
    private Long packageId;
    private String packageName;
    private Boolean menuCheckStrictly;
    private String status;
    private String remark;
    private List<Long> appIds;
    private List<Long> menuIds;
    private LocalDateTime createTime;
}
