package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 抽检大棚选择项。 */
@Data
public class SfStaskInspectionGreenhouseFieldVo {

    /** 大棚 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long greenhouseId;
    /** 大棚编码。 */
    private String greenhouseCode;
    /** 大棚名称。 */
    private String greenhouseName;
    /** 当前活跃种植批次的物种 ID，无批次时为空。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cropId;
    /** 当前活跃种植批次的物种名称，无批次时为空。 */
    private String cropName;
}
