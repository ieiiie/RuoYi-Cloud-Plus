package com.ym.agriculture.farming.farmrecord.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 农事记录地块明细，对应 {@code sf_farming_record_field}。
 *
 * @author ym-cloud
 */
@Data
@TableName("sf_farming_record_field")
public class SfFarmingRecordField implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录地块明细主键。
     */
    @TableId("id")
    private Long id;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 农事记录 ID。
     */
    private Long recordId;

    /**
     * 地块 ID。
     */
    private Long fieldId;

    /**
     * 地块编号快照。
     */
    private String fieldCodeSnapshot;

    /**
     * 地块名称快照。
     */
    private String fieldNameSnapshot;

    /**
     * 种植日期快照，取地块当前活跃种植批次的 sowing_date。
     */
    private Date sowingDateSnapshot;

    /**
     * 记录内排序，越小越靠前。
     */
    private Integer sortOrder;
}
