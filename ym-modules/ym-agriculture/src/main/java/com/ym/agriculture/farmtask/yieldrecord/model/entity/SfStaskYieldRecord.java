package com.ym.agriculture.farmtask.yieldrecord.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 产量记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_yield_record")
public class SfStaskYieldRecord extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "yield_id", type = IdType.ASSIGN_ID)
    private Long yieldId;

    private LocalDate harvestDate;

    private Long speciesId;

    private Long varietyId;

    private String speciesNameSnapshot;

    private String varietyNameSnapshot;

    private BigDecimal yieldKg;

    @TableLogic
    private String delFlag;
}
