package com.ym.agriculture.farmtask.worker.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * stask 小程序组长管理农事视图。
 */
@Data
public class SfStaskLeaderManagedFarmWorkVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.FARM_WORK_DICT,
        idProperty = "workItemId", fieldKey = "dictName")
    private String workItemName;

    /**
     * 农事分类ID。
     */
    private Long categoryId;

    /**
     * 农事分类名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.FARM_WORK_DICT,
        idProperty = "categoryId", fieldKey = "dictName")
    private String categoryName;

    /**
     * 该农事项目下由组长管理的大棚列表。
     */
    private List<SfStaskManagedGreenhouseVo> greenhouses;
}
