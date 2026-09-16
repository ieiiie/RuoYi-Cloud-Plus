package com.ym.agriculture.farming.farmrecord.model.vo;

import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordType;
import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 农事类型（查询/列表出参）。
 */
@Data
@AutoMapper(target = SfFarmingRecordType.class)
public class SfFarmingRecordTypeVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 类型主键 {@code type_id} */
    private Long typeId;

    /** 租户内稳定编码。 */
    private String typeCode;

    /** 展示名。 */
    private String typeName;

    /** 列表/下拉用图标 URL，可空 */
    private String listIconUrl;

    /** 排序，数值越小越靠前 */
    private Integer sortOrder;

    /** {@code 0} 正常 {@code 1} 停用，与档案表 {@code sys_dict/normal} 风格一致 */
    private String status;
}
