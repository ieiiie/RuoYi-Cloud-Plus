package com.ym.agriculture.farmtask.workorder.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * stask 任务包农事项编辑回显视图对象。
 */
@Data
public class SfStaskPackageItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 任务包农事项明细ID。
     */
    private Long itemId;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "itemId", fieldKey = "workItemNameSnapshot")
    private String workItemNameSnapshot;

    /**
     * 农事项目编码快照。
     */
    private String workItemCodeSnapshot;

    /**
     * 农事分类ID快照。
     */
    private Long categoryIdSnapshot;

    /**
     * 农事分类名称快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "itemId", fieldKey = "categoryNameSnapshot")
    private String categoryNameSnapshot;

    /**
     * 选择的大棚ID列表。
     */
    private List<Long> greenhouseIds;

    /**
     * 选择的大棚列表，含 ID 与名称。
     */
    private List<SfStaskGreenhouseBriefVo> greenhouses;

    /**
     * 申请人作业要求。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "itemId", fieldKey = "managerRequirement")
    private String managerRequirement;

    /**
     * 申请人照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String managerPhotos;

    /**
     * 技术员针对农事项说明。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "itemId", fieldKey = "techInstruction")
    private String techInstruction;

    /**
     * 技术员参考照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String techPhotos;

    /** 当前农事项在整个任务包中的物资总量快照。 */
    private List<SfStaskTaskMaterialVo> materials;

    /**
     * 组长分配模式：AUTO/MANUAL。
     */
    private String leaderAssignMode;

    /**
     * 手动指派组长员工ID。
     */
    private Long manualLeaderId;

    /**
     * 手动指派组长姓名快照。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "manualLeaderNameSnapshotResourceId", fieldKey = "manualLeaderNameSnapshot")
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "manualLeaderNameLeaderSnapshotResourceId", fieldKey = "leaderNameSnapshot")
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "manualLeaderNameEmployeeResourceId", fieldKey = "name")
    private String manualLeaderName;

    /** 手动组长姓名实际来自手动快照时的资源 ID。 */
    @JsonIgnore
    private Long manualLeaderNameSnapshotResourceId;

    /** 手动组长姓名回退最终组长快照时的资源 ID。 */
    @JsonIgnore
    private Long manualLeaderNameLeaderSnapshotResourceId;

    /** 手动组长姓名回退当前人员时的资源 ID。 */
    @JsonIgnore
    private Long manualLeaderNameEmployeeResourceId;

    /**
     * 最终有效组长员工ID；AUTO/MANUAL 均返回。
     */
    private Long leaderId;

    /**
     * 最终有效组长姓名；AUTO/MANUAL 均返回。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_WORK_ORDER_ITEM,
        idProperty = "leaderNameSnapshotResourceId", fieldKey = "leaderNameSnapshot")
    @StaskI18nField(resourceType = I18nResourceType.SYSTEM_EMPLOYEE,
        idProperty = "leaderNameEmployeeResourceId", fieldKey = "name")
    private String leaderName;

    /**
     * 该农事项和有效组长关联的领料单ID；无物料或未正式提交时为 null。
     */
    private Long materialReceiptId;

    /**
     * 该农事项和有效组长关联的领料单号；无物料或未正式提交时为 null。
     */
    private String materialReceiptNo;

    /** 最终组长姓名实际来自历史快照时的资源 ID。 */
    @JsonIgnore
    private Long leaderNameSnapshotResourceId;

    /** 最终组长姓名回退当前人员时的资源 ID。 */
    @JsonIgnore
    private Long leaderNameEmployeeResourceId;
}
