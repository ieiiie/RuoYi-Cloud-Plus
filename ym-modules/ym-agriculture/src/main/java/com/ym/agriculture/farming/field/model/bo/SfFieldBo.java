package com.ym.agriculture.farming.field.model.bo;

import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.layout.model.bo.GreenhouseLayoutDraftBo;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 地块档案业务对象。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SfField.class, reverseConvertGenerate = false)
public class SfFieldBo extends BaseEntity {

    @NotNull(message = "地块ID不能为空", groups = EditGroup.class)
    private Long fieldId;

    private Long ownerUserId;

    @Size(max = 64, message = "地块编码不能超过64个字符")
    private String fieldCode;

    @NotBlank(message = "地块名称不能为空", groups = { AddGroup.class, EditGroup.class })
    @Size(max = 200, message = "地块名称不能超过200个字符")
    private String fieldName;

    @Size(max = 32, message = "大棚简称不能超过32个字符")
    private String greenhouseShortName;

    @Pattern(regexp = "^$|^#[0-9A-Fa-f]{6}$", message = "大棚颜色必须为#RRGGBB格式")
    private String greenhouseColor;

    private Integer sortOrder;

    @Pattern(regexp = "^(FIELD|GREENHOUSE)$", message = "地块类型只能为FIELD或GREENHOUSE")
    private String fieldType;

    private String boundaryGeojson;

    @DecimalMin(value = "-180", message = "经度不能小于-180")
    @DecimalMax(value = "180", message = "经度不能大于180")
    private BigDecimal centerLng;

    @DecimalMin(value = "-90", message = "纬度不能小于-90")
    @DecimalMax(value = "90", message = "纬度不能大于90")
    private BigDecimal centerLat;

    @DecimalMin(value = "0", inclusive = false, message = "面积必须大于0")
    private BigDecimal areaMu;

    @Size(max = 500, message = "地址不能超过500个字符")
    private String addressText;

    @Size(max = 500, message = "行政区划不能超过500个字符")
    private String adminDivisionText;

    @Size(max = 32, message = "行政区划编码不能超过32个字符")
    private String adminDivisionAdcode;

    private Integer mapZoomLevel;

    @Pattern(regexp = "^[01]$", message = "状态只能为0或1")
    private String status;

    @Pattern(regexp = "^(IDLE|IN_USE|MAINTENANCE)$", message = "地块业务状态不正确")
    private String fieldStatus;

    @Size(max = 32, message = "地图服务商不能超过32个字符")
    private String mapProvider;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    /** 大棚类型地块在新增、编辑时可同步提交整体布局。 */
    private GreenhouseLayoutDraftBo greenhouseLayoutDraft;
}
