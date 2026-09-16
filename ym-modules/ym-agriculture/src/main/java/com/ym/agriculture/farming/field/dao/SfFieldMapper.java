package com.ym.agriculture.farming.field.dao;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;
import java.util.Collection;

/**
 * 地块档案数据层。
 *
 * @author ym-cloud
 */
public interface SfFieldMapper extends BaseMapperPlus<SfField, SfFieldVo> {

    default void applyFieldListOrder(LambdaQueryWrapper<SfField> wrapper) {
        wrapper.orderByAsc(SfField::getSortOrder).orderByAsc(SfField::getFieldId);
    }

    default LambdaQueryWrapper<SfField> buildQueryWrapper(SfFieldBo bo) {
        LambdaQueryWrapper<SfField> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(SfField::getDelFlag, SystemConstants.NORMAL)
            .eq(ObjectUtil.isNotNull(bo.getFieldId()), SfField::getFieldId, bo.getFieldId())
            .eq(ObjectUtil.isNotNull(bo.getOwnerUserId()), SfField::getOwnerUserId, bo.getOwnerUserId())
            .like(StringUtils.isNotBlank(bo.getFieldCode()), SfField::getFieldCode, bo.getFieldCode())
            .like(StringUtils.isNotBlank(bo.getFieldName()), SfField::getFieldName, bo.getFieldName())
            .eq(StringUtils.isNotBlank(bo.getFieldType()), SfField::getFieldType, bo.getFieldType())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SfField::getStatus, bo.getStatus())
            .eq(StringUtils.isNotBlank(bo.getFieldStatus()), SfField::getFieldStatus, bo.getFieldStatus())
            .eq(StringUtils.isNotBlank(bo.getAdminDivisionAdcode()), SfField::getAdminDivisionAdcode,
                bo.getAdminDivisionAdcode())
            .like(StringUtils.isNotBlank(bo.getAdminDivisionText()), SfField::getAdminDivisionText,
                bo.getAdminDivisionText())
            .orderByAsc(SfField::getSortOrder)
            .orderByDesc(SfField::getCreateTime)
            .orderByDesc(SfField::getFieldId);
        return wrapper;
    }

    default List<SfFieldVo> selectFieldList(SfFieldBo bo) {
        return selectVoList(buildQueryWrapper(bo));
    }

    default Page<SfFieldVo> selectFieldPage(Page<SfField> page, SfFieldBo bo) {
        return selectVoPage(page, buildQueryWrapper(bo));
    }

    default List<SfFieldVo> selectEnabledByIds(Collection<Long> fieldIds) {
        LambdaQueryWrapper<SfField> wrapper = Wrappers.lambdaQuery();
        wrapper.in(fieldIds != null && !fieldIds.isEmpty(), SfField::getFieldId, fieldIds)
            .eq(SfField::getStatus, SystemConstants.NORMAL)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfField::getSortOrder)
            .orderByAsc(SfField::getFieldId);
        return selectVoList(wrapper);
    }

    default List<SfFieldVo> selectVoByFieldIds(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return selectVoList(Wrappers.<SfField>lambdaQuery()
            .in(SfField::getFieldId, fieldIds)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
    }

    default List<SfField> selectNormalEntitiesByFieldIds(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfField>lambdaQuery()
            .in(SfField::getFieldId, fieldIds)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean existsByFieldCode(String fieldCode, Long excludeFieldId) {
        if (StringUtils.isBlank(fieldCode)) {
            return false;
        }
        return exists(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getFieldCode, fieldCode)
            .ne(ObjectUtil.isNotNull(excludeFieldId), SfField::getFieldId, excludeFieldId)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean existsByFieldName(String fieldName, Long excludeFieldId) {
        if (StringUtils.isBlank(fieldName)) {
            return false;
        }
        return exists(Wrappers.<SfField>lambdaQuery()
            .eq(SfField::getFieldName, fieldName)
            .ne(ObjectUtil.isNotNull(excludeFieldId), SfField::getFieldId, excludeFieldId)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL));
    }

    default boolean updateStatus(Long fieldId, String status) {
        return update(null, Wrappers.<SfField>lambdaUpdate()
            .eq(SfField::getFieldId, fieldId)
            .eq(SfField::getDelFlag, SystemConstants.NORMAL)
            .set(SfField::getStatus, status)) > 0;
    }
}
