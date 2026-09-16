package com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.tenant.core.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.time.LocalDate;

/**
 * 巡查照片归档日期，表 {@code sf_inspection_photo_archive}。
 *
 * @author ym-cloud
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sf_inspection_photo_archive")
public class SfInspectionPhotoArchive extends TenantEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 归档主键。 */
    @TableId(value = "archive_id", type = IdType.ASSIGN_ID)
    private Long archiveId;

    /** 巡查归档日期，租户内唯一，格式 yyyy-MM-dd。 */
    private LocalDate archiveDate;
}
