package com.ym.iot.jetlinks.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.jetlinks.domain.JetLinksBusinessEvent;
import com.ym.iot.jetlinks.domain.dto.ProjectionRow;

import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

/** 物理业务库访问。全局设备目录、历史归属与任务查询使用显式业务条件， 不继承调用方租户拦截条件；访问权限在服务层校验，SQL 值使用绑定参数。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface JetLinksEventMapper
        extends BaseMapperPlus<JetLinksBusinessEvent, JetLinksBusinessEvent> {
    @Update(
            """
            UPDATE iot_jetlinks_event_failure
                    SET state='RESOLVED',updated_at=CURRENT_TIMESTAMP(3)
                    WHERE event_id=#{eventId}
            """)
    int resolveFailure(@Param("eventId") String eventId);

    @Insert(
            """
            INSERT INTO iot_jetlinks_event_failure(event_id,consumer_id,event_type,payload_json,state,attempts,last_error,updated_at)
                    VALUES(#{eventId},#{consumerId},#{eventType},#{payload},'FAILED',1,#{error},CURRENT_TIMESTAMP(3))
                    ON DUPLICATE KEY UPDATE state='FAILED',attempts=attempts+1,last_error=VALUES(last_error),updated_at=CURRENT_TIMESTAMP(3)
            """)
    int recordFailure(
            @Param("eventId") String eventId,
            @Param("consumerId") String consumerId,
            @Param("eventType") String eventType,
            @Param("payload") String payload,
            @Param("error") String error);

    @Select(
            """
            SELECT *
                    FROM iot_jetlinks_event_failure
                    WHERE event_id=#{eventId}
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> selectFailure(
            @Param("eventId") String eventId);

    @Update(
            """
            UPDATE iot_jetlinks_event_failure
                    SET state='REPLAYED_ACK_PENDING',replay_by=#{operator},updated_at=CURRENT_TIMESTAMP(3)
                    WHERE event_id=#{eventId}
            """)
    int markReplayed(@Param("operator") String operator, @Param("eventId") String eventId);

    @Select(
            """
            SELECT event_id,consumer_id,event_type,state,attempts,last_error,replay_by,updated_at
                    FROM iot_jetlinks_event_failure
                    WHERE state<>'RESOLVED'
                    ORDER BY updated_at DESC
                    LIMIT #{limit}
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> selectUnresolvedFailures(
            @Param("limit") int limit);

    @Insert(
            """
            INSERT IGNORE INTO iot_jetlinks_business_event(event_id,event_type,device_id,source_time,payload_json,created_at)
                    VALUES(#{eventId},#{eventType},#{deviceId},#{sourceTime},#{payload},CURRENT_TIMESTAMP(3))
            """)
    int insertReceipt(
            @Param("eventId") String eventId,
            @Param("eventType") String eventType,
            @Param("deviceId") String deviceId,
            @Param("sourceTime") long sourceTime,
            @Param("payload") String payload);

    @Select(
            """
            SELECT tenant_id
                    FROM iot_device_ownership_history
                    WHERE device_id=#{deviceId}
                    AND assignment_version=#{version}
                    AND effective_from<=#{from}
                    AND (effective_to IS NULL
                    OR effective_to>#{to})
            """)
    List<String> selectSourceTenants(
            @Param("deviceId") Long deviceId,
            @Param("version") Long version,
            @Param("from") Date from,
            @Param("to") Date to);

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
                    ON DUPLICATE KEY UPDATE
                    <choose>
                        <when test="!row.updates.isEmpty()">
                            <foreach collection="row.updates" index="column" item="value" separator=",">
                                ${column}=VALUES(${column})
                            </foreach>
                        </when>
                        <otherwise>
                            ${row.key}=${row.key}
                        </otherwise>
                    </choose>
            </script>
            """)
    int upsertHistory(@Param("row") ProjectionRow row);
}
