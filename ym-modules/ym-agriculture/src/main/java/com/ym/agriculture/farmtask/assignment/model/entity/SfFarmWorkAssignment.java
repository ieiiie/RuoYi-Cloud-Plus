package com.ym.agriculture.farmtask.assignment.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * stask 农事分配记录，表 {@code sf_farm_work_assignment}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_farm_work_assignment")
public class SfFarmWorkAssignment extends TenantEntity {

    /**
     * 农事分配主键。
     */
    @TableId("assignment_id")
    private Long assignmentId;

    /**
     * 大棚ID，对应 {@code sf_field.field_id} 且地块类型为 GREENHOUSE。
     */
    private Long greenhouseId;

    /**
     * 农事项目ID，对应 {@code sf_farm_work_dict.dict_id} 且节点类型为 ITEM。
     */
    private Long workItemId;

    /**
     * 农事项目名称快照，用于项目物理删除后继续回显。
     */
    private String workItemNameSnapshot;

    /**
     * 农事项目编码快照，用于项目物理删除后继续回显。
     */
    private String workItemCodeSnapshot;

    /**
     * 农事分类ID快照，用于分类物理删除后继续回显。
     */
    private Long categoryIdSnapshot;

    /**
     * 农事分类名称快照，用于分类物理删除后继续回显。
     */
    private String categoryNameSnapshot;

    /**
     * 组长人员ID，对应 {@code sys_employee.employee_id}。
     */
    private Long leaderId;

    /**
     * 分配时间。
     */
    private Date assignedAt;
}
