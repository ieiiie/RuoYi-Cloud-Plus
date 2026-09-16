package com.ym.iot.fertilizer.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 施肥机控制操作日志，对应 {@code iot_fertilizer_control_log}。
 *
 * <p>用于记录变更类控制操作的审计链路，日志写入失败不影响设备控制。</p>
 */
@Data
@TableName("iot_fertilizer_control_log")
public class IotFertilizerControlLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 主键（雪花 ID） */
    @TableId(value = "log_id")
    private Long logId;

    /** 租户 ID，取设备档案 {@code iot_device.tenant_id} */
    private String tenantId;

    /** 设备主键（iot_device.id） */
    private Long deviceId;

    /** 设备编号（冗余，方便查询） */
    private String deviceCode;

    /** 操作人用户 ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;

    /** 操作人 IP */
    private String operatorIp;

    /** 命令标识 */
    private String command;

    /** 异步任务 ID */
    private String taskId;

    /** 最终状态：SUCCEEDED/FAILED/TIMEOUT */
    private String status;

    /** 错误信息 */
    private String errorMessage;

    /** 请求体 JSON */
    private String requestBody;

    /** 急停原因 */
    private String emergencyReason;

    /** 下行帧 HEX，多条按换行追加 */
    private String downlinkFrame;

    /** 下行 MQTT topic，多条按换行追加 */
    private String downlinkTopic;

    /** 操作开始时间 */
    private Date beginAt;

    /** 操作结束时间 */
    private Date endAt;

    /** 操作耗时（毫秒） */
    private Integer durationMs;

    /** 记录创建时间 */
    private Date createTime;
}
