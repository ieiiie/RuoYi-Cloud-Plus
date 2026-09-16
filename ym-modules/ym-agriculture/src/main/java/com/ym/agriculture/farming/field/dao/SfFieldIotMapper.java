package com.ym.agriculture.farming.field.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.field.model.entity.SfFieldIot;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * 地块设备关联数据层。
 *
 * @author ym-cloud
 */
public interface SfFieldIotMapper extends BaseMapperPlus<SfFieldIot, SfFieldIot> {

    default List<SfFieldIot> selectNormalListByFieldId(Long fieldId) {
        return selectList(Wrappers.<SfFieldIot>lambdaQuery()
            .eq(SfFieldIot::getFieldId, fieldId)
            .eq(SfFieldIot::getDelFlag, SystemConstants.NORMAL)
            .orderByAsc(SfFieldIot::getId));
    }

    default List<SfFieldIot> selectNormalListByFieldIds(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfFieldIot>lambdaQuery()
            .in(SfFieldIot::getFieldId, fieldIds)
            .eq(SfFieldIot::getDelFlag, SystemConstants.NORMAL));
    }

    default long countNormalByFieldIdsAndDeviceSn(Collection<Long> fieldIds, String deviceSn) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return 0L;
        }
        return selectCount(Wrappers.<SfFieldIot>lambdaQuery()
            .in(SfFieldIot::getFieldId, fieldIds)
            .eq(SfFieldIot::getDeviceSn, deviceSn)
            .eq(SfFieldIot::getDelFlag, SystemConstants.NORMAL));
    }

    default Long selectFieldIdByDeviceSn(String deviceSn) {
        SfFieldIot row = selectOne(Wrappers.<SfFieldIot>lambdaQuery()
            .eq(SfFieldIot::getDeviceSn, deviceSn)
            .eq(SfFieldIot::getDelFlag, SystemConstants.NORMAL)
            .last("LIMIT 1"));
        return row == null ? null : row.getFieldId();
    }

    default boolean existsByFieldIdAndDeviceSn(Long fieldId, String deviceSn) {
        return exists(Wrappers.<SfFieldIot>lambdaQuery()
            .eq(SfFieldIot::getFieldId, fieldId)
            .eq(SfFieldIot::getDeviceSn, deviceSn)
            .eq(SfFieldIot::getDelFlag, SystemConstants.NORMAL));
    }

    /** 仅供内部设备变更守卫使用；归属变更必须检查遗留在其他租户的绑定。 */
    @com.baomidou.mybatisplus.annotation.InterceptorIgnore(tenantLine = "true", dataPermission = "true")
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM sf_field_iot WHERE device_sn=#{deviceSn} AND del_flag='0'")
    long countAllActiveBindings(@Param("deviceSn") String deviceSn);

    @Delete("DELETE FROM sf_field_iot WHERE tenant_id = #{tenantId} AND field_id = #{fieldId}")
    int deleteByFieldId(@Param("tenantId") String tenantId, @Param("fieldId") Long fieldId);

    @Delete("<script>DELETE FROM sf_field_iot WHERE tenant_id = #{tenantId} AND field_id IN "
        + "<foreach collection='fieldIds' item='fieldId' open='(' separator=',' close=')'>"
        + "#{fieldId}</foreach></script>")
    int deleteByFieldIds(@Param("tenantId") String tenantId,
                         @Param("fieldIds") Collection<Long> fieldIds);

    @Delete("DELETE FROM sf_field_iot WHERE tenant_id = #{tenantId} "
        + "AND field_id = #{fieldId} AND device_sn = #{deviceSn}")
    int deleteByFieldIdAndDeviceSn(@Param("tenantId") String tenantId,
                                   @Param("fieldId") Long fieldId,
                                   @Param("deviceSn") String deviceSn);
}
