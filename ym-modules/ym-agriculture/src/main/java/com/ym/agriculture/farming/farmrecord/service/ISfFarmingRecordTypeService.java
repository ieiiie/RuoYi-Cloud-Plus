package com.ym.agriculture.farming.farmrecord.service;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordTypeSaveBo;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordTypeVo;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 农事类型主数据（多端共用存储）。
 */
@Validated
public interface ISfFarmingRecordTypeService {

    List<SfFarmingRecordTypeVo> listEnabled();

    List<SfFarmingRecordTypeVo> listAllNormal();

    SfFarmingRecordTypeVo get(@NotNull Long typeId);

    boolean add(@Validated(AddGroup.class) SfFarmingRecordTypeSaveBo bo);

    boolean update(@Validated(EditGroup.class) SfFarmingRecordTypeSaveBo bo);

    boolean remove(@NotNull Long typeId);
}
