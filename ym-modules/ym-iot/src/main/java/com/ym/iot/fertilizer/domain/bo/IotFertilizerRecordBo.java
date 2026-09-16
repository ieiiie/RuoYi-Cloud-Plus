package com.ym.iot.fertilizer.domain.bo;

import lombok.Data;

import java.util.Date;

/**
 * 施肥流水查询参数。
 *
 * @author ym-cloud
 */
@Data
public class IotFertilizerRecordBo {

    /** 平台设备主键。 */
    private Long deviceId;

    /** 设备编号（与 deviceId 二选一过滤）。 */
    private String deviceCode;

    /** 施肥类型（uint16 取低 8 位，0 表示未指定）。 */
    private Integer fertilizationType;

    /** 施肥时间下限（含）。 */
    private Date beginTime;

    /** 施肥时间上限（含）。 */
    private Date endTime;
}
