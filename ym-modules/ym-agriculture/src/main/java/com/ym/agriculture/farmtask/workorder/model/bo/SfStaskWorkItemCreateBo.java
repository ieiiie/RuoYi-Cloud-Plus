package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * stask 任务包农事项创建入参。
 */
@Data
public class SfStaskWorkItemCreateBo {

    /**
     * 农事项目ID。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_WORK_ITEM_REQUIRED + "}")
    private Long workItemId;

    /**
     * 选择的大棚ID列表。
     */
    @NotEmpty(message = "{" + StaskMessageKeys.VALIDATION_GREENHOUSES_REQUIRED + "}")
    private List<Long> greenhouseIds;

    /**
     * 申请人作业要求，最多500字。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_MANAGER_REQUIREMENT_MAX + "}")
    private String managerRequirement;

    /**
     * 申请人照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String managerPhotos;

    /**
     * 技术员针对农事项说明，最多500字。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_TECH_INSTRUCTION_MAX + "}")
    private String techInstruction;

    /**
     * 技术员参考照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String techPhotos;

    /**
     * 当前农事项在整个任务包中的物资总量。生产管理员任务会忽略该字段；
     * 技术员任务仅在农事项 requiresMaterial=true 时允许配置。
     */
    @Valid
    private List<SfStaskTaskMaterialBo> materials;

    /**
     * 组长分配模式：AUTO-自动分配，MANUAL-手动指派；不传按 AUTO。
     */
    private String leaderAssignMode;

    /**
     * 手动指派组长员工ID；leaderAssignMode=MANUAL 时必填。
     */
    private Long manualLeaderId;
}
