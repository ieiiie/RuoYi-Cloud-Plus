package com.ym.iot.jetlinks.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.jetlinks.domain.JetLinksProjectionState;
import com.ym.iot.jetlinks.domain.dto.ProjectionRow;

import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/** 物理业务库访问。全局设备目录、历史归属与任务查询使用显式业务条件， 不继承调用方租户拦截条件；访问权限在服务层校验，SQL 值使用绑定参数。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface JetLinksProjectionMapper
        extends BaseMapperPlus<JetLinksProjectionState, JetLinksProjectionState> {
    @Insert(
            """
            INSERT IGNORE INTO iot_jetlinks_projection(projection_key,business_id,source_time,version,payload_json)
                    VALUES(#{key},#{businessId},-1,-1,'{}')
            """)
    int initializeProjection(@Param("key") String key, @Param("businessId") long businessId);

    @Select(
            """
            SELECT *
                    FROM iot_jetlinks_projection
                    WHERE projection_key=#{key}
                    FOR UPDATE
            """)
    Map<String, Object> lockProjection(@Param("key") String key);

    @Update(
            """
            UPDATE iot_jetlinks_projection
                    SET source_time=#{sourceTime},version=#{version},payload_json=#{payload}
                    WHERE projection_key=#{key}
            """)
    int updateProjection(
            @Param("sourceTime") long sourceTime,
            @Param("version") long version,
            @Param("payload") String payload,
            @Param("key") String key);

    @Select(
            """
            SELECT record_id
                    FROM iot_fertilizer_record
                    WHERE device_id=#{deviceId}
                    AND tenant_id=#{tenantId}
                    AND record_time=#{time}
                    AND LOWER(raw_payload_hex)=LOWER(#{raw})
            """)
    List<Long> findLegacyFertilizerRecord(
            @Param("deviceId") Long deviceId,
            @Param("tenantId") String tenantId,
            @Param("time") Date time,
            @Param("raw") String raw);

    @Update(
            """
            UPDATE iot_jetlinks_projection
                    SET business_id=#{businessId}
                    WHERE projection_key=#{key}
            """)
    int bindLegacyRecord(@Param("businessId") Long businessId, @Param("key") String key);

    @Select(
            """
            SELECT *
                    FROM iot_jetlinks_command_task
                    WHERE request_id=#{requestId}
                    OR command_id=#{commandId}
                    FOR UPDATE
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> lockCommandTasks(
            @Param("requestId") String requestId, @Param("commandId") String commandId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET command_id=#{commandId},state=#{state},result_json=#{result},updated_at=#{updatedAt}
                    WHERE request_id=#{requestId}
            """)
    int updateCommandFact(
            @Param("commandId") String commandId,
            @Param("state") String state,
            @Param("result") String result,
            @Param("updatedAt") Date updatedAt,
            @Param("requestId") Object requestId);

    @Select(
            """
            SELECT *
                    FROM iot_jetlinks_command_task
                    WHERE request_id=#{requestId}
                    AND function_id='fertilizer.task'
                    FOR UPDATE
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> lockFertilizerTask(
            @Param("requestId") String requestId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET state=#{state},command_id=NULL,result_json=#{result},updated_at=#{updatedAt}
                    WHERE request_id=#{requestId}
            """)
    int confirmFertilizerStep(
            @Param("state") Object state,
            @Param("result") String result,
            @Param("updatedAt") Date updatedAt,
            @Param("requestId") Object requestId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET state='FAILED',command_id=#{commandId},error_message=#{error},updated_at=#{updatedAt}
                    WHERE request_id=#{requestId}
            """)
    int failFertilizerStep(
            @Param("commandId") String commandId,
            @Param("error") String error,
            @Param("updatedAt") Date updatedAt,
            @Param("requestId") Object requestId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET state=#{state},command_id=#{commandId},updated_at=#{updatedAt}
                    WHERE request_id=#{requestId}
            """)
    int updateFertilizerStep(
            @Param("state") Object state,
            @Param("commandId") String commandId,
            @Param("updatedAt") Date updatedAt,
            @Param("requestId") Object requestId);

    @Select(
            """
            SELECT *
                    FROM iot_motorvalve_valve_session
                    WHERE device_id=#{deviceId}
                    AND tenant_id=#{tenantId}
                    AND valve_no=#{valveNo}
                    AND status='OPEN'
                    ORDER BY open_time DESC
                    FOR UPDATE
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> lockOpenValveSessions(
            @Param("deviceId") Long deviceId,
            @Param("tenantId") String tenantId,
            @Param("valveNo") int valveNo);

    @Update(
            """
            UPDATE iot_motorvalve_valve_session
                    SET open_control_log_id=COALESCE(open_control_log_id,#{logId}),open_by=COALESCE(open_by,#{operator})
                    WHERE id=#{sessionId}
            """)
    int attachOpenControl(
            @Param("logId") Long logId,
            @Param("operator") Long operator,
            @Param("sessionId") Object sessionId);

    @Update(
            """
            UPDATE iot_motorvalve_valve_session
                    SET status=#{status},close_time=#{closeTime},duration_seconds=#{duration},close_position=#{position},close_control_log_id=#{logId},close_by=#{operator},remark=#{remark},update_time=#{updatedAt}
                    WHERE id=#{sessionId}
            """)
    int closeValveSession(
            @Param("status") Object status,
            @Param("closeTime") Date closeTime,
            @Param("duration") int duration,
            @Param("position") int position,
            @Param("logId") Long logId,
            @Param("operator") Long operator,
            @Param("remark") Object remark,
            @Param("updatedAt") Date updatedAt,
            @Param("sessionId") Object sessionId);

    @Update(
            """
            UPDATE iot_device
                    SET del_flag='2'
                    WHERE device_id=#{deviceId}
            """)
    int archiveDevice(@Param("deviceId") Long deviceId);

    @Select(
            """
            SELECT device_code
                    FROM iot_device
                    WHERE device_id=#{deviceId}
            """)
    List<String> selectDeviceCodes(@Param("deviceId") Long deviceId);

    @Select(
            """
            <script>
            SELECT tenant_id,assignment_version
                    FROM iot_device_ownership_history
                    WHERE device_id=#{deviceId}
                    <if test="version != null and version &gt; 0">
                        AND assignment_version=#{version}
                    </if>
                    <if test="checkTime">
                        AND effective_from &lt;= #{time}
                        AND (effective_to IS NULL
                        OR effective_to &gt; #{time})
                    </if>
            </script>
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> selectSourceOwners(
            @Param("deviceId") Long deviceId,
            @Param("version") Long version,
            @Param("checkTime") boolean checkTime,
            @Param("time") Date time);

    @Select(
            """
            SELECT tenant_id
                    FROM ${row.table}
                    WHERE ${row.key}=#{row.id}
                    FOR UPDATE
            """)
    List<String> selectTenantForUpdate(@Param("row") ProjectionRow row);

    @Insert(
            """
            <script>
            INSERT INTO ${row.table} (
                    <foreach collection="row.fields" index="column" item="value" separator=",">
                        ${column}
                    </foreach>
                    )
                    VALUES (
                    <foreach collection="row.fields" item="value" separator=",">
                        #{value}
                    </foreach>
                    )
            </script>
            """)
    int insertRow(@Param("row") ProjectionRow row);

    @Update(
            """
            <script>
            UPDATE ${row.table}
                    SET
                    <foreach collection="row.updates" index="column" item="value" separator=",">
                        ${column}=#{value}
                    </foreach>
                    WHERE ${row.key}=#{row.id}
            </script>
            """)
    int updateRow(@Param("row") ProjectionRow row);
}
