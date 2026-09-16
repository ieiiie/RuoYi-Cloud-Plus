package com.ym.agriculture.farmtask.sop.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.sop.model.entity.SfStaskSop;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/** 农事 SOP 数据访问。 */
@Mapper
public interface SfStaskSopMapper extends BaseMapperPlus<SfStaskSop, SfStaskSop> {

    /** 查询同一农事、作物范围的两种语言 SOP。 */
    default List<SfStaskSop> selectByMatchKey(String tenantId, Long workItemId, Long cropSpeciesId) {
        return selectList(Wrappers.<SfStaskSop>lambdaQuery()
            .eq(SfStaskSop::getTenantId, tenantId)
            .eq(SfStaskSop::getWorkItemId, workItemId)
            .eq(SfStaskSop::getCropSpeciesId, cropSpeciesId));
    }
}
