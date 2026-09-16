package com.ym.agriculture.farming.batch.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 检测仪信息视图：聚合 IoT 设备基础信息、所在地块、当前种植品种及最新采集日期。
 *
 * @author ym-cloud
 */
@Data
public class SfDetectorInfoVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备主键（iot_device.device_id） */
    private Long deviceId;

    /** 仪器种类（iot_device.device_category） */
    private String deviceCategory;

    /** 检测仪型号（iot_device.device_code） */
    private String deviceCode;

    /** 检测仪名称（iot_device.device_name） */
    private String deviceName;

    /** 所在地块 ID */
    private Long fieldId;

    /** 所在地块名称 */
    private String fieldName;

    /** 当前种植品种 ID（活跃批次关联） */
    private Long varietyId;

    /** 当前种植品种名称 */
    private String varietyName;

    /** 最新数据采集日期（iot_device.last_report_time） */
    private Date lastDataDate;
}
