package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import com.ym.agriculture.shared.i18n.StaskI18nComposite;
import com.ym.agriculture.farmtask.i18n.StaskI18nTextCompositions;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * stask 大棚简要信息视图对象。
 */
@Data
public class SfStaskGreenhouseBriefVo implements Serializable, StaskI18nComposite {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工单大棚明细主键，仅用于服务端定位翻译资源。
     */
    @JsonIgnore
    private Long greenhouseItemId;

    /**
     * 大棚ID，对应 {@code sf_field.field_id}。
     */
    private Long greenhouseId;

    /**
     * 大棚编号快照。
     */
    private String greenhouseCode;

    /**
     * 大棚名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_GREENHOUSE,
        idProperty = "greenhouseItemId", fieldKey = "greenhouseNameSnapshot")
    private String greenhouseName;

    /**
     * 活跃种植批次作物名，多个作物按当前语言的列表分隔符拼接。
     */
    private String cropName;

    /**
     * 该大棚进行中的种植批次列表（PLANNING/PLANTING/GROWING/HARVESTING）；无则为空数组。
     */
    private List<SfPlantingBatchVo> plantingBatches;

    /** 使用已本地化的批次作物名称重建维文展示值。 */
    @Override
    public void rebuildLocalizedText() {
        cropName = StaskI18nTextCompositions.cropNames(plantingBatches, "، ");
    }
}
