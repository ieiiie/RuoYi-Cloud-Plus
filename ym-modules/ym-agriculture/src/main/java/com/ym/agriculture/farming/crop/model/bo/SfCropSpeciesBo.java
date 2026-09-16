package com.ym.agriculture.farming.crop.model.bo;

import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import com.ym.common.mybatis.core.domain.BaseEntity;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 作物品类业务对象。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@AutoMapper(target = SfCropSpecies.class, reverseConvertGenerate = false)
public class SfCropSpeciesBo extends BaseEntity {

    @NotNull(message = "品类ID不能为空", groups = EditGroup.class)
    private Long speciesId;

    @NotBlank(message = "品类编码不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 64, message = "品类编码不能超过64个字符")
    private String speciesCode;

    @NotBlank(message = "品类名称不能为空", groups = {AddGroup.class, EditGroup.class})
    @Size(max = 100, message = "品类名称不能超过100个字符")
    private String speciesName;

    private Integer remoteSensingCode;

    @Pattern(regexp = "^[01]$", message = "状态只能为0或1")
    private String status;

    @Size(max = 512, message = "图标地址不能超过512个字符")
    private String mapIconUrl;

    @Size(max = 16, message = "Emoji不能超过16个字符")
    private String mapIconEmoji;

    private Map<String, Object> growthStageConfig;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;
}
