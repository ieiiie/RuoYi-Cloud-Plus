package com.ym.agriculture.farming.tenantinit.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_crop_species_template")
public class SfCropSpeciesTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("template_id")
    private Long templateId;

    private String templateCode;
    private String speciesCode;
    private String speciesName;
    private Integer remoteSensingCode;
    private String status;
    private Integer sortOrder;
    private String mapIconUrl;
    private String mapIconEmoji;
    private String growthStageConfigJson;
    private String remark;
}
