package com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;

/**
 * 巡查归档中的大棚快照，表 {@code sf_inspection_photo_archive_field}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_inspection_photo_archive_field")
public class SfInspectionPhotoArchiveField extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档大棚快照主键。 */
    @TableId(value = "archive_field_id", type = IdType.ASSIGN_ID)
    private Long archiveFieldId;

    /** 所属归档日期主键 {@code sf_inspection_photo_archive.archive_id}。 */
    private Long archiveId;

    /** 来源大棚主键 {@code sf_field.field_id}。 */
    private Long fieldId;

    /** 创建归档时的大棚编号快照。 */
    private String fieldCode;

    /** 创建归档时的大棚名称快照。 */
    private String fieldName;

    /** 创建归档时的大棚排序值，越小越靠前。 */
    private Integer sortOrder;

    /** 创建归档时选中的进行中种植批次主键；未种植时为空。 */
    private Long plantingBatchId;

    /** 创建归档时的作物物种主键；未种植时为空。 */
    private Long speciesId;

    /** 创建归档时的作物物种名称快照；未种植时为空。 */
    private String speciesName;

    /** 创建归档时的作物品种主键；未种植时为空。 */
    private Long varietyId;

    /** 创建归档时的作物品种名称快照；未种植时为空。 */
    private String varietyName;
}
