package com.ym.agriculture.farmtask.inspection.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/** 抽检命令结果。 */
@Data
public class SfStaskInspectionMutationVo {

    /** 抽检记录 ID。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long inspectionId;
    /** 更新后的状态编码。 */
    private String status;
    /** 更新后的状态本地化名称。 */
    private String statusLabel;
    /** 更新后的成功处理次数。 */
    private Integer handleCount;
    /** 更新后的乐观锁版本。 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long version;
}
