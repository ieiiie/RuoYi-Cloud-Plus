package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminPackageQueryBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskQueryBo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminPackageListRow;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskReadListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 后台任务查看页专用分页 SQL。 */
@Mapper
public interface SfStaskAdminReadMapper {

    @Select("""
        <script>
        SELECT o.order_id AS orderId, o.order_no AS orderNo, o.package_id AS packageId,
               p.package_no AS packageNo, o.creator_employee_id AS creatorEmployeeId,
               o.creator_role_code AS creatorRoleCode, o.plan_date AS planDate, o.status AS status,
               o.greenhouse_id AS greenhouseId, o.greenhouse_name_snapshot AS greenhouseNameSnapshot,
               o.work_item_id AS workItemId, o.work_item_name_snapshot AS workItemNameSnapshot,
               o.leader_id AS leaderId, o.required_worker_count AS requiredWorkerCount,
               o.accepted_worker_count AS acceptedWorkerCount, o.create_time AS createTime
        FROM sf_stask_work_order o
        LEFT JOIN sf_stask_task_package p ON p.tenant_id = o.tenant_id AND p.package_id = o.package_id
        WHERE o.tenant_id = #{tenantId}
          <if test="bo.packageId != null">AND o.package_id = #{bo.packageId}</if>
          <if test="bo.status != null and bo.status != ''">AND o.status = #{bo.status}</if>
          <if test="bo.greenhouseId != null">AND o.greenhouse_id = #{bo.greenhouseId}</if>
          <if test="bo.workItemId != null">AND o.work_item_id = #{bo.workItemId}</if>
          <if test="bo.leaderId != null">AND o.leader_id = #{bo.leaderId}</if>
          <if test="bo.planStartDate != null">AND o.plan_date <![CDATA[>=]]> #{bo.planStartDate}</if>
          <if test="bo.planEndDate != null">AND o.plan_date <![CDATA[<=]]> #{bo.planEndDate}</if>
          <if test="bo.keyword != null and bo.keyword != ''">
            AND (CAST(o.order_id AS CHAR) LIKE CONCAT('%', #{bo.keyword}, '%')
              OR o.order_no LIKE CONCAT('%', #{bo.keyword}, '%')
              OR CAST(o.package_id AS CHAR) LIKE CONCAT('%', #{bo.keyword}, '%')
              OR o.greenhouse_name_snapshot LIKE CONCAT('%', #{bo.keyword}, '%')
              OR o.work_item_name_snapshot LIKE CONCAT('%', #{bo.keyword}, '%'))
          </if>
        ORDER BY o.create_time DESC, o.order_id DESC
        </script>
        """)
    Page<SfStaskAdminTaskReadListRow> selectAdminTasks(Page<SfStaskAdminTaskReadListRow> page,
        @Param("tenantId") String tenantId, @Param("bo") SfStaskAdminTaskQueryBo bo);

    @Select("""
        <script>
        SELECT p.package_id AS packageId, p.package_no AS packageNo,
               p.creator_employee_id AS creatorEmployeeId, p.creator_role_code AS creatorRoleCode,
               p.plan_date AS planDate, p.status AS status,
               (SELECT COUNT(1) FROM sf_stask_work_order o
                WHERE o.tenant_id = p.tenant_id AND o.package_id = p.package_id) AS taskCount,
               p.create_time AS createTime
        FROM sf_stask_task_package p
        WHERE p.tenant_id = #{tenantId}
          <if test="bo.status != null and bo.status != ''">AND p.status = #{bo.status}</if>
          <if test="bo.creatorEmployeeId != null">AND p.creator_employee_id = #{bo.creatorEmployeeId}</if>
          <if test="bo.creatorRoleCode != null and bo.creatorRoleCode != ''">AND p.creator_role_code = #{bo.creatorRoleCode}</if>
          <if test="bo.planStartDate != null">AND p.plan_date <![CDATA[>=]]> #{bo.planStartDate}</if>
          <if test="bo.planEndDate != null">AND p.plan_date <![CDATA[<=]]> #{bo.planEndDate}</if>
          <if test="bo.keyword != null and bo.keyword != ''">
            AND (CAST(p.package_id AS CHAR) LIKE CONCAT('%', #{bo.keyword}, '%')
              OR p.package_no LIKE CONCAT('%', #{bo.keyword}, '%'))
          </if>
        ORDER BY p.create_time DESC, p.package_id DESC
        </script>
        """)
    Page<SfStaskAdminPackageListRow> selectAdminPackages(Page<SfStaskAdminPackageListRow> page,
        @Param("tenantId") String tenantId, @Param("bo") SfStaskAdminPackageQueryBo bo);
}
