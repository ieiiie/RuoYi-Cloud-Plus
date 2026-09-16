package com.ym.agriculture.farming.trace.model.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import com.ym.agriculture.farming.trace.model.entity.SfTraceBatch;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 溯源批次业务对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SfTraceBatch.class, reverseConvertGenerate = false)
public class SfTraceBatchBo extends BaseEntity {

    @NotNull(message = "溯源批次ID不能为空", groups = EditGroup.class)
    private Long traceBatchId;

    private Long plantingBatchId;

    private Long fieldId;
    private Long varietyId;

    @NotBlank(message = "溯源批次号不能为空", groups = AddGroup.class)
    @Size(max = 64, message = "溯源批次号长度不能超过{max}个字符")
    private String traceBatchNo;

    @NotBlank(message = "商品名称不能为空", groups = AddGroup.class)
    @Size(max = 100, message = "商品名称长度不能超过{max}个字符")
    private String productName;

    private String qualityGrade;

    @Size(max = 255, message = "产地长度不能超过{max}个字符")
    private String originText;

    @Size(max = 200, message = "生产单位长度不能超过{max}个字符")
    private String producerName;

    private String certificationJson;
    private String labelScope;
    private Integer plannedQuantity;
    private String status;
    private String remark;

    /** 查询：商品名称模糊 */
    private String productNameLike;
}
