package com.ym.agriculture.farming.batch.model.bo;

import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 种植批次请求与查询条件。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SfPlantingBatch.class, reverseConvertGenerate = false)
public class SfPlantingBatchBo extends BaseEntity {

    @NotNull(message = "批次ID不能为空", groups = EditGroup.class)
    private Long batchId;

    @NotNull(message = "地块不能为空", groups = AddGroup.class)
    private Long fieldId;

    @NotNull(message = "品种不能为空", groups = AddGroup.class)
    private Long varietyId;

    @NotBlank(message = "批次编号不能为空", groups = AddGroup.class)
    @Size(max = 64, message = "批次编号长度不能超过{max}个字符")
    private String batchCode;

    @NotNull(message = "第几茬不能为空", groups = AddGroup.class)
    @Min(value = 1, message = "第几茬最小为1", groups = {AddGroup.class, EditGroup.class})
    @Max(value = 10, message = "第几茬最大为10", groups = {AddGroup.class, EditGroup.class})
    private Integer croppingIndex;

    @NotNull(message = "播种日期不能为空", groups = AddGroup.class)
    private Date sowingDate;

    private Date expectedHarvestDate;
    private Date actualHarvestDate;
    private String batchStatus;
    private String remark;
    private String fieldName;
    private String varietyName;
    private Long speciesId;
    private Date sowingDateBegin;
    private Date sowingDateEnd;
}
