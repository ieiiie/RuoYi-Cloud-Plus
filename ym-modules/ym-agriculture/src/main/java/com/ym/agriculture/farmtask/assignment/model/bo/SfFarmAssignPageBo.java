package com.ym.agriculture.farmtask.assignment.model.bo;

import com.ym.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * stask 农事分配大棚列表查询入参。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfFarmAssignPageBo extends BaseEntity {

    /**
     * 大棚编号，模糊匹配 {@code sf_field.field_code}。
     */
    private String greenhouseCode;

    /**
     * 大棚名称，模糊匹配 {@code sf_field.field_name}。
     */
    private String greenhouseName;

    /**
     * 是否只看需分配大棚：true-仅返回未分配农事数大于0的大棚。
     */
    private Boolean onlyUnassigned;
}
