package com.ym.agriculture.farming.crop.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 作物品类视图对象。
 */
@Data
@AutoMapper(target = SfCropSpecies.class)
public class SfCropSpeciesVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long speciesId;
    private String tenantId;
    private String speciesCode;
    private String speciesName;
    private Integer remoteSensingCode;
    private String status;
    private String mapIconUrl;
    private String mapIconEmoji;
    private Map<String, Object> growthStageConfig;
    @JsonIgnore
    private String growthStageConfigJson;
    private Date createTime;
    private Date updateTime;
    private String remark;
}
