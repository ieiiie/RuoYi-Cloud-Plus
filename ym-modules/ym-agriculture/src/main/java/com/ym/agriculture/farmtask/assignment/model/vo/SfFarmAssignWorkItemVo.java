package com.ym.agriculture.farmtask.assignment.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * stask 农事分配项目展示出参。
 */
@Data
public class SfFarmAssignWorkItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分配记录ID；待分配区为空。
     */
    private Long assignmentId;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_ASSIGNMENT,
        idProperty = "assignmentId", fieldKey = "workItemNameSnapshot")
    @StaskI18nField(resourceType = I18nResourceType.FARM_WORK_DICT,
        idProperty = "workItemId", fieldKey = "dictName")
    private String workItemName;

    /**
     * 农事项目编码。
     */
    private String workItemCode;

    /**
     * 农事分类ID。
     */
    private Long categoryId;

    /**
     * 农事分类名称。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_ASSIGNMENT,
        idProperty = "assignmentId", fieldKey = "categoryNameSnapshot")
    @StaskI18nField(resourceType = I18nResourceType.FARM_WORK_DICT,
        idProperty = "categoryId", fieldKey = "dictName")
    private String categoryName;
}
