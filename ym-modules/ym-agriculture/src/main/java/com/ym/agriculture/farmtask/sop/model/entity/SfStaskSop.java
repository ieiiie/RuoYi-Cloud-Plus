package com.ym.agriculture.farmtask.sop.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 农事标准作业规程，表 {@code sf_stask_sop}。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_stask_sop")
public class SfStaskSop extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** SOP 主键。 */
    @TableId(value = "sop_id", type = IdType.ASSIGN_ID)
    private Long sopId;

    /** 农事项目 ID。 */
    private Long workItemId;

    /** 作物范围：ALL/SPECIFIC。 */
    private String cropScope;

    /** 作物物种 ID；0 表示全部作物。 */
    private Long cropSpeciesId;

    /** 内容语言：zh-CN/ug-CN。 */
    private String language;

    /** 有序内容块 JSON。 */
    private String contentBlocks;
}
