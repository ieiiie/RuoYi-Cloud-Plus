package com.ym.agriculture.farmtask.inspection.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspection.model.entity.SfStaskInspection;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionListVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 抽检记录 Mapper。 */
@Mapper
public interface SfStaskInspectionMapper extends BaseMapperPlus<SfStaskInspection, SfStaskInspectionListVo> {

    /**
     * 绕过逻辑删除查询单条记录，仅用于将已删除记录转换为稳定 410 错误。
     */
    @Select("SELECT * FROM sf_stask_inspection "
        + "WHERE tenant_id = #{tenantId} AND inspection_id = #{inspectionId} LIMIT 1")
    SfStaskInspection selectIncludingDeleted(@Param("tenantId") String tenantId,
        @Param("inspectionId") Long inspectionId);

    default long countUnprocessedByTechnician(String tenantId, Long employeeId, String status) {
        return selectCount(Wrappers.<SfStaskInspection>lambdaQuery()
            .eq(SfStaskInspection::getTenantId, tenantId)
            .eq(SfStaskInspection::getDelFlag, SystemConstants.NORMAL)
            .eq(SfStaskInspection::getResponsibleTechnicianEmployeeId, employeeId)
            .eq(SfStaskInspection::getStatus, status));
    }

    default long countUnprocessedByLeader(String tenantId, Long employeeId, String status) {
        return selectCount(Wrappers.<SfStaskInspection>lambdaQuery()
            .eq(SfStaskInspection::getTenantId, tenantId)
            .eq(SfStaskInspection::getDelFlag, SystemConstants.NORMAL)
            .eq(SfStaskInspection::getResponsibleLeaderEmployeeId, employeeId)
            .eq(SfStaskInspection::getStatus, status));
    }
}
