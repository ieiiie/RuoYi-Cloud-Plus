package com.ym.agriculture.farmtask.workorder.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * stask 任务包农事项明细，表 {@code sf_stask_work_order_item}。
 */
@Data
@TableName("sf_stask_work_order_item")
public class SfStaskWorkOrderItem {

    /**
     * 任务包农事项明细ID。
     */
    @TableId("item_id")
    private Long itemId;

    /**
     * 任务包ID。
     */
    private Long packageId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 农事项目ID。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照。
     */
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
    private String categoryNameSnapshot;

    /**
     * 申请人作业要求。
     */
    private String managerRequirement;

    /**
     * 申请人照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String managerPhotos;

    /**
     * 技术员针对农事项说明。
     */
    private String techInstruction;

    /**
     * 技术员参考照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String techPhotos;

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
    private String manualLeaderNameSnapshot;

    /**
     * 最终有效组长员工ID快照；AUTO/MANUAL 均保存。
     */
    private Long leaderIdSnapshot;

    /**
     * 最终有效组长姓名快照；AUTO/MANUAL 均保存。
     */
    private String leaderNameSnapshot;

    /**
     * 任务包内排序。
     */
    private Integer sortOrder;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;
}
