package com.ym.agriculture.farming.crop.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 作物品类详情。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfCropSpeciesDetailVo extends SfCropSpeciesVo {

    private Long varietyCount;
    private List<SfCropVarietyVo> varietyList;
}
