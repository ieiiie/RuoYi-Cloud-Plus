package com.ym.agriculture.farming.farmrecord.model.bo;

import com.ym.common.core.validate.AddGroup;
import com.ym.common.core.validate.EditGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 农事类型新增/修改入参。
 */
@Data
public class SfFarmingRecordTypeSaveBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 修改时必填，主键。 */
    @NotNull(message = "类型主键不能为空", groups = {EditGroup.class})
    private Long typeId;

    /**
     * 租户内唯一编码；字母数字与下划线建议，创建后若已被农事引用请谨慎改名（当前实现允许编码变更）。
     */
    @NotBlank(message = "类型编码不能为空", groups = {AddGroup.class, EditGroup.class})
    private String typeCode;

    /** 展示名称。 */
    @NotBlank(message = "类型名称不能为空", groups = {AddGroup.class, EditGroup.class})
    private String typeName;

    /** 列表/Tab 图标 URL，可空。 */
    private String listIconUrl;

    /** 排序，升序；可空等价于 0。 */
    private Integer sortOrder;

    /**
     * 状态：{@code 0} 正常、{@code 1} 停用（与全局字典惯例一致）。
     */
    private String status;

    /** 备注，可空。 */
    private String remark;
}
