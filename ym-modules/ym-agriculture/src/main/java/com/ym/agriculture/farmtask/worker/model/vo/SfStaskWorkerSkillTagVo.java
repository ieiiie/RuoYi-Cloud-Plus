package com.ym.agriculture.farmtask.worker.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * stask 小程序人员技能标签视图。
 */
@Data
public class SfStaskWorkerSkillTagVo implements Serializable {

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
     * 作物类型编码，ALL 表示全部作物。
     */
    private String cropType;

    /**
     * 作物类型名称。
     */
    private String cropTypeName;

    /**
     * 技能等级：ADVANCED-高级，MEDIUM-中级，JUNIOR-初级。
     */
    private String skillLevel;

    /**
     * 技能等级名称。
     */
    private String skillLevelName;

    /**
     * 累计从事次数。
     */
    private Integer workCount;
}
