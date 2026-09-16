package com.ym.agriculture.farmtask.workorder.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAdminTaskListQueryBo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskAdminTaskListRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 平台后台农事任务联合分页 Mapper。
 */
@Mapper
public interface SfStaskAdminTaskListMapper {

    /**
     * 在数据库中联合分页未拆分任务包和拆分工单。
     *
     * <p>任务包分支使用同租户 {@code NOT EXISTS} 排除已拆分任务包，关联筛选使用 {@code EXISTS}
     * 避免明细表连接造成重复行。</p>
     *
     * @param page     分页参数
     * @param tenantId 当前租户编号
     * @param bo       查询条件
     * @return 当前页联合任务行
     */
    @Select("""
        <script>
        SELECT task_rows.task_type AS taskType,
               task_rows.record_id AS recordId,
               task_rows.package_id AS packageId,
               task_rows.package_no AS packageNo,
               task_rows.order_id AS orderId,
               task_rows.order_no AS orderNo,
               task_rows.creator_employee_id AS creatorEmployeeId,
               task_rows.creator_role_code AS creatorRoleCode,
               task_rows.plan_date AS planDate,
               task_rows.status AS status,
               task_rows.greenhouse_id AS greenhouseId,
               task_rows.greenhouse_name_snapshot AS greenhouseNameSnapshot,
               task_rows.work_item_id AS workItemId,
               task_rows.work_item_name_snapshot AS workItemNameSnapshot,
               task_rows.leader_id AS leaderId,
               task_rows.required_worker_count AS requiredWorkerCount,
               task_rows.create_time AS createTime
        FROM (
            SELECT 'PACKAGE' AS task_type,
                   p.package_id AS record_id,
                   p.package_id,
                   p.package_no,
                   NULL AS order_id,
                   NULL AS order_no,
                   p.creator_employee_id,
                   p.creator_role_code,
                   p.plan_date,
                   p.status,
                   NULL AS greenhouse_id,
                   NULL AS greenhouse_name_snapshot,
                   NULL AS work_item_id,
                   NULL AS work_item_name_snapshot,
                   NULL AS leader_id,
                   NULL AS required_worker_count,
                   p.create_time
            FROM sf_stask_task_package p
            WHERE p.tenant_id = #{tenantId}
              AND NOT EXISTS (
                  SELECT 1
                  FROM sf_stask_work_order split_order
                  WHERE split_order.tenant_id = p.tenant_id
                    AND split_order.package_id = p.package_id
              )
              <if test="bo.status != null and bo.status != ''">
                AND p.status = #{bo.status}
              </if>
              <if test="bo.creatorEmployeeId != null">
                AND p.creator_employee_id = #{bo.creatorEmployeeId}
              </if>
              <if test="bo.planStartDate != null">
                AND p.plan_date <![CDATA[>=]]> #{bo.planStartDate}
              </if>
              <if test="bo.planEndDate != null">
                AND p.plan_date <![CDATA[<=]]> #{bo.planEndDate}
              </if>
              <if test="bo.workItemId != null">
                AND EXISTS (
                    SELECT 1
                    FROM sf_stask_work_order_item package_item
                    WHERE package_item.tenant_id = p.tenant_id
                      AND package_item.package_id = p.package_id
                      AND package_item.work_item_id = #{bo.workItemId}
                )
              </if>
              <if test="bo.greenhouseId != null">
                AND EXISTS (
                    SELECT 1
                    FROM sf_stask_work_order_greenhouse package_greenhouse
                    WHERE package_greenhouse.tenant_id = p.tenant_id
                      AND package_greenhouse.package_id = p.package_id
                      AND package_greenhouse.greenhouse_id = #{bo.greenhouseId}
                )
              </if>

            UNION ALL

            SELECT 'SPLIT' AS task_type,
                   o.order_id AS record_id,
                   o.package_id,
                   NULL AS package_no,
                   o.order_id,
                   o.order_no,
                   o.creator_employee_id,
                   o.creator_role_code,
                   o.plan_date,
                   o.status,
                   o.greenhouse_id,
                   o.greenhouse_name_snapshot,
                   o.work_item_id,
                   o.work_item_name_snapshot,
                   o.leader_id,
                   o.required_worker_count,
                   o.create_time
            FROM sf_stask_work_order o
            WHERE o.tenant_id = #{tenantId}
              <if test="bo.status != null and bo.status != ''">
                AND o.status = #{bo.status}
              </if>
              <if test="bo.creatorEmployeeId != null">
                AND o.creator_employee_id = #{bo.creatorEmployeeId}
              </if>
              <if test="bo.planStartDate != null">
                AND o.plan_date <![CDATA[>=]]> #{bo.planStartDate}
              </if>
              <if test="bo.planEndDate != null">
                AND o.plan_date <![CDATA[<=]]> #{bo.planEndDate}
              </if>
              <if test="bo.workItemId != null">
                AND o.work_item_id = #{bo.workItemId}
              </if>
              <if test="bo.greenhouseId != null">
                AND (
                    o.greenhouse_id = #{bo.greenhouseId}
                    OR EXISTS (
                        SELECT 1
                        FROM sf_stask_work_order_greenhouse split_greenhouse
                        WHERE split_greenhouse.tenant_id = o.tenant_id
                          AND split_greenhouse.order_id = o.order_id
                          AND split_greenhouse.greenhouse_id = #{bo.greenhouseId}
                    )
                )
              </if>
        ) task_rows
        ORDER BY task_rows.create_time DESC, task_rows.record_id DESC
        </script>
        """)
    Page<SfStaskAdminTaskListRow> selectAdminPage(
        Page<SfStaskAdminTaskListRow> page,
        @Param("tenantId") String tenantId,
        @Param("bo") SfStaskAdminTaskListQueryBo bo);
}
