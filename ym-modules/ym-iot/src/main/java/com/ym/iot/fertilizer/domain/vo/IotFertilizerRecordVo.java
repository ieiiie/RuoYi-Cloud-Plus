package com.ym.iot.fertilizer.domain.vo;

import com.ym.iot.fertilizer.domain.IotFertilizerRecord;

import io.github.linpeilie.annotations.AutoMapper;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 施肥流水视图对象 iot_fertilizer_record。
 *
 * @author ym-cloud
 */
@Data
@AutoMapper(target = IotFertilizerRecord.class)
public class IotFertilizerRecordVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long recordId;

    private String tenantId;

    private Long deviceId;

    private String deviceCode;

    private Date recordTime;

    private Integer fertilizationType;

    private Integer fertilizationSeconds;

    private BigDecimal fertilizationQuantity;

    private String rawPayloadHex;

    private Date createTime;
}
