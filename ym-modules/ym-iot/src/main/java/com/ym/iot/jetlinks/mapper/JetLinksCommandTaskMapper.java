package com.ym.iot.jetlinks.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.jetlinks.domain.JetLinksCommandTask;
import com.ym.iot.jetlinks.domain.dto.JetLinksTaskSnapshot;

import org.apache.ibatis.annotations.*;

import java.util.Date;
import java.util.List;

/** 物理业务库访问。全局设备目录、历史归属与任务查询使用显式业务条件， 不继承调用方租户拦截条件；访问权限在服务层校验，SQL 值使用绑定参数。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface JetLinksCommandTaskMapper
        extends BaseMapperPlus<JetLinksCommandTask, JetLinksCommandTask> {
    @Insert(
            """
            INSERT INTO iot_jetlinks_command_task(request_id,device_id,tenant_id,assignment_version,function_id,state,inputs_json,priority,created_at,updated_at)
                    VALUES(#{requestId},#{deviceId},#{tenantId},#{version},#{functionId},'QUEUED',#{inputs},#{priority},CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
            """)
    int createPriorityTask(
            @Param("requestId") String requestId,
            @Param("deviceId") Long deviceId,
            @Param("tenantId") String tenantId,
            @Param("version") Long version,
            @Param("functionId") String functionId,
            @Param("inputs") String inputs,
            @Param("priority") int priority);

    @Select(
            """
            SELECT inputs_json
                    FROM iot_jetlinks_command_task
                    WHERE request_id=#{requestId}
            """)
    String selectInputs(@Param("requestId") String requestId);

    @Select(
            """
            SELECT error_message
                    FROM iot_jetlinks_command_task
                    WHERE request_id=#{requestId}
            """)
    String selectError(@Param("requestId") String requestId);

    @Select(
            """
            SELECT *
                    FROM iot_jetlinks_command_task
                    WHERE request_id=#{requestId}
                    FOR UPDATE
            """)
    List<org.springframework.util.LinkedCaseInsensitiveMap<Object>> lockTask(
            @Param("requestId") String requestId);

    @Select(
            """
            SELECT device_id
                    FROM iot_device
                    WHERE device_id=#{deviceId}
                    FOR UPDATE
            """)
    List<Long> lockDevice(@Param("deviceId") Long deviceId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET command_id=NULL,result_json=#{result},state=#{state},updated_at=CURRENT_TIMESTAMP(3)
                    WHERE request_id=#{requestId}
            """)
    int completeStep(
            @Param("result") String result,
            @Param("state") Object state,
            @Param("requestId") String requestId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET command_id=#{commandId},state='RUNNING',updated_at=CURRENT_TIMESTAMP(3)
                    WHERE request_id=#{requestId}
            """)
    int markStepRunning(@Param("commandId") String commandId, @Param("requestId") String requestId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET state=#{state},error_message=#{error},updated_at=CURRENT_TIMESTAMP(3)
                    WHERE request_id=#{requestId}
            """)
    int finishTask(
            @Param("state") String state,
            @Param("error") String error,
            @Param("requestId") String requestId);

    @Insert(
            """
            INSERT INTO iot_jetlinks_command_task(request_id,device_id,tenant_id,assignment_version,function_id,state,inputs_json,created_at,updated_at)
                    VALUES(#{requestId},#{deviceId},#{tenantId},#{version},#{functionId},'QUEUED',#{inputs},CURRENT_TIMESTAMP(3),CURRENT_TIMESTAMP(3))
            """)
    int createTask(
            @Param("requestId") String requestId,
            @Param("deviceId") Long deviceId,
            @Param("tenantId") String tenantId,
            @Param("version") Long version,
            @Param("functionId") String functionId,
            @Param("inputs") String inputs);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET command_id=#{commandId},state=#{state},result_json=#{result},updated_at=#{updatedAt}
                    WHERE request_id=#{requestId}
                    AND updated_at<=#{expectedTime}
            """)
    int updateCommand(
            @Param("commandId") String commandId,
            @Param("state") String state,
            @Param("result") String result,
            @Param("updatedAt") Date updatedAt,
            @Param("requestId") String requestId,
            @Param("expectedTime") Date expectedTime);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET state='FAILED',error_message=#{error},updated_at=CURRENT_TIMESTAMP(3)
                    WHERE request_id=#{requestId}
                    AND state='QUEUED'
            """)
    int markFailed(@Param("error") String error, @Param("requestId") String requestId);

    @Update(
            """
            UPDATE iot_jetlinks_command_task
                    SET state='UNKNOWN',error_message=#{error},updated_at=CURRENT_TIMESTAMP(3)
                    WHERE request_id=#{requestId}
                    AND state NOT IN ('SUCCEEDED','FAILED','TIMED_OUT')
            """)
    int markUnknown(@Param("error") String error, @Param("requestId") String requestId);

    @Select(
            """
            SELECT *
                    FROM iot_jetlinks_command_task
                    WHERE request_id=#{requestId}
                    OR command_id=#{commandId}
            """)
    List<JetLinksTaskSnapshot> selectTask(
            @Param("requestId") String requestId, @Param("commandId") String commandId);

    @Select(
            """
            SELECT request_id
                    FROM iot_jetlinks_command_task
                    WHERE device_id=#{deviceId}
                    AND tenant_id=#{tenantId}
                    AND assignment_version=#{version}
                    AND function_id LIKE 'fertilizer.%'
                    AND state NOT IN ('SUCCEEDED','FAILED','TIMED_OUT')
                    ORDER BY created_at DESC
                    LIMIT 1
            """)
    List<String> selectActiveTaskIds(
            @Param("deviceId") Long deviceId,
            @Param("tenantId") String tenantId,
            @Param("version") Long version);

    @Select(
            """
            SELECT request_id FROM iot_jetlinks_command_task
            WHERE function_id='fertilizer.task' AND state IN ('QUEUED','RUNNING')
              AND request_id>#{afterId}
            ORDER BY request_id LIMIT 100
            """)
    List<String> selectRecoveryTasks(@Param("afterId") String afterId);
}
