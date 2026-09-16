package com.ym.agriculture.farming.crop.model.vo;

import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 作物品种视图对象。
 */
@Data
@AutoMapper(target = SfCropVariety.class)
public class SfCropVarietyVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long varietyId;
    private String tenantId;
    private Long speciesId;
    private String speciesName;
    private Integer remoteSensingCode;
    private String varietyCode;
    private String varietyName;
    private Integer growthCycleDays;
    private String status;
    private String mapIconUrl;
    private String mapIconEmoji;
    private Date createTime;
    private Date updateTime;
    private String remark;
}
