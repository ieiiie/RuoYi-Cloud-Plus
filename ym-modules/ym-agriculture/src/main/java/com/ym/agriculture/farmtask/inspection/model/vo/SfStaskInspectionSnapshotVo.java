package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 大棚当前种植快照。 */
@Data
public class SfStaskInspectionSnapshotVo {

    /** 大棚 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long greenhouseId;
    /** 大棚名称。 */
    private String greenhouseName;
    /** 当前活跃种植批次 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long plantingId;
    /** 物种 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long cropId;
    /** 物种名称。 */
    private String cropName;
    /** 品种 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long varietyId;
    /** 品种名称。 */
    private String varietyName;
    /** 种植批次状态。 */
    private String plantingStatus;
}
