package com.ym.agriculture.farming.field.model.vo;

import com.ym.agriculture.farming.field.model.entity.SfField;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 地块档案视图。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = SfField.class)
public class SfFieldVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long fieldId;
    private String tenantId;
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
    private Date createTime;
    private Date updateTime;
    private String remark;
    private String mapDisplayStatus;
    private String fieldStatusDisplay;
    private Long uavBindingId;
    private String uavWorkspaceName;
}
