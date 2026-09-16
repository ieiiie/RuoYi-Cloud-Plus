package com.ym.iot.ownership.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.ownership.domain.IotDeviceOwnership;
import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;
import com.ym.iot.ownership.domain.dto.OwnershipInterval;
import com.ym.iot.ownership.domain.vo.AssignedDeviceVo;
import com.ym.iot.ownership.domain.vo.OwnershipHistoryVo;
import com.ym.iot.ownership.domain.vo.UnassignedDeviceVo;

import org.apache.ibatis.annotations.*;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/** 物理业务库访问。全局设备目录、历史归属与任务查询使用显式业务条件， 不继承调用方租户拦截条件；访问权限在服务层校验，SQL 值使用绑定参数。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface DeviceOwnershipMapper
        extends BaseMapperPlus<IotDeviceOwnership, DeviceOwnershipSnapshot> {
    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_device
                    WHERE device_id=#{deviceId}
                    AND del_flag='0'
            """)
    Long countDevice(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT *
                    FROM iot_device_ownership
                    WHERE device_id=#{deviceId}
            """)
    List<DeviceOwnershipSnapshot> selectSnapshot(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT device_id
                    FROM iot_device
                    WHERE device_id=#{deviceId}
                    AND del_flag='0'
                    FOR SHARE
            """)
    List<Long> lockExistingDevice(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT *
                    FROM iot_device_ownership
                    WHERE device_id=#{deviceId}
                    FOR SHARE
            """)
    List<DeviceOwnershipSnapshot> lockSnapshot(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT d.device_id,d.device_code,d.device_name,COALESCE(o.assignment_version,0) assignment_version,COALESCE(o.fence_status,'ACTIVE') fence_status
                    FROM iot_device d
                    LEFT JOIN iot_device_ownership o ON o.device_id=d.device_id
                    WHERE d.del_flag='0'
                    AND o.tenant_id IS NULL
                    AND d.device_id>#{afterId}
                    ORDER BY d.device_id
                    LIMIT #{limit}
            """)
    List<UnassignedDeviceVo> selectUnassigned(
            @Param("afterId") long afterId, @Param("limit") int limit);

    @Select(
            """
            SELECT *
                    FROM iot_device_ownership_history
                    WHERE device_id=#{deviceId}
                    AND assignment_version>#{afterVersion}
                    ORDER BY assignment_version
                    LIMIT #{limit}
            """)
    List<OwnershipHistoryVo> selectHistory(
            @Param("deviceId") Long deviceId,
            @Param("afterVersion") long afterVersion,
            @Param("limit") int limit);

    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_motorvalve_valve_session
                    WHERE device_id=#{deviceId}
                    AND (status='OPEN'
                    OR (close_time IS NULL
                    AND (status IS NULL
                    OR status<>'ABORTED')))
            """)
    Long countOpenValves(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_motorvalve_control_log
                    WHERE device_id=#{deviceId}
                    AND (status IS NULL
                    OR status NOT IN ('SUCCESS','FAILED','REJECTED'))
            """)
    Long countPendingValveCommands(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_fertilizer_control_log
                    WHERE device_id=#{deviceId}
                    AND (status IS NULL
                    OR status NOT IN ('SUCCEEDED','FAILED','TIMEOUT')
                    OR end_at IS NULL)
            """)
    Long countPendingFertilizerCommands(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_jetlinks_command_task
                    WHERE device_id=#{deviceId}
                    AND (state IS NULL
                    OR state NOT IN ('SUCCEEDED','FAILED','TIMED_OUT'))
            """)
    Long countPendingCoreCommands(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_device_ownership_history
                    WHERE device_id=#{deviceId}
                    AND tenant_id=#{tenantId}
                    AND effective_from<=#{from}
                    AND (effective_to IS NULL
                    OR effective_to>#{to})
            """)
    Long countVisibleInterval(
            @Param("deviceId") String deviceId,
            @Param("tenantId") String tenantId,
            @Param("from") Date from,
            @Param("to") Date to);

    @Select(
            """
            <script>
            SELECT o.device_id
                    FROM iot_device_ownership o
                    JOIN iot_device d ON d.device_id=o.device_id
                    WHERE o.tenant_id=#{tenant}
                    AND o.fence_status='ACTIVE'
                    AND d.del_flag='0'
                    <if test="ids != null">
                        <choose>
                            <when test="!ids.isEmpty()">
                                AND o.device_id IN
                                <foreach collection="ids" item="id" open="(" separator="," close=")">
                                    #{id}
                                </foreach>
                            </when>
                            <otherwise>
                                AND 1=0
                            </otherwise>
                        </choose>
                    </if>
                    ORDER BY o.device_id
            </script>
            """)
    List<Long> selectAccessible(@Param("tenant") String tenant, @Param("ids") Collection<Long> ids);

    @Select(
            """
            <script>
            SELECT d.device_id,d.device_code,d.device_name,o.tenant_id,o.assignment_version,o.effective_from,o.fence_status
                    FROM iot_device d
                    JOIN iot_device_ownership o ON o.device_id=d.device_id
                    WHERE d.del_flag='0'
                    AND o.tenant_id IS NOT NULL
                    AND d.device_id &gt; #{afterId}
                    <if test="tenant != null and !tenant.isBlank()">
                        AND o.tenant_id=#{tenant}
                    </if>
                    ORDER BY d.device_id
                    LIMIT #{limit}
            </script>
            """)
    List<AssignedDeviceVo> selectAssigned(
            @Param("afterId") long afterId,
            @Param("limit") int limit,
            @Param("tenant") String tenant);

    @Select(
            """
            SELECT device_id,effective_from,effective_to
                    FROM iot_device_ownership_history
                    WHERE tenant_id=#{tenant}
                    ORDER BY device_id,effective_from
            """)
    List<OwnershipInterval> selectTenantIntervals(@Param("tenant") String tenant);

    @Select(
            """
            SELECT device_id,assignment_version
                    FROM iot_device_ownership
                    WHERE tenant_id=#{tenant}
                    AND fence_status='ACTIVE'
                    ORDER BY device_id
            """)
    List<IotDeviceOwnership> selectAlarmAssignments(@Param("tenant") String tenant);

    @Select("SELECT device_code FROM iot_device WHERE device_id=#{deviceId} AND del_flag='0'")
    String selectDeviceCode(@Param("deviceId") Long deviceId);
}
