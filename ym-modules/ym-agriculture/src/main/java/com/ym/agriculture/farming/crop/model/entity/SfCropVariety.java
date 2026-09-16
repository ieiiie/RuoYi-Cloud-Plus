package com.ym.agriculture.farming.crop.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 作物品种实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_crop_variety")
public class SfCropVariety extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("variety_id")
    private Long varietyId;
    private Long speciesId;
    private String varietyCode;
    private String varietyName;
    private Integer growthCycleDays;
    private String status;

    @TableLogic
    private String delFlag;

    private String mapIconUrl;
    private String mapIconEmoji;
    private String remark;
}
