package com.ym.agriculture.farming.crop.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 作物品类与品种两层树。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfCropSpeciesTreeVo extends SfCropSpeciesVo {

    private List<SfCropVarietyVo> children = new ArrayList<>();
}
