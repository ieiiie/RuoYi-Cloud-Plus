package com.ym.agriculture.farming.field.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.math.BigDecimal;

/**
 * 地块档案实体。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_field")
public class SfField extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "field_id")
    private Long fieldId;

    private Long ownerUserId;
    private String fieldCode;
    private String fieldName;
    private String greenhouseShortName;
    private String greenhouseColor;
    private Integer sortOrder;
    private String fieldType;
    private String boundaryGeojson;
    private BigDecimal centerLng;
    private BigDecimal centerLat;
    private BigDecimal areaMu;
    private String addressText;
    private String adminDivisionText;
    private String adminDivisionAdcode;
    private Integer mapZoomLevel;
    private String status;
    private String fieldStatus;
    private String mapProvider;

    @TableLogic
    private String delFlag;

    private String remark;
}
