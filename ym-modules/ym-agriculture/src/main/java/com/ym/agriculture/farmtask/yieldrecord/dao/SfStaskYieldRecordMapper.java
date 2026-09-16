package com.ym.agriculture.farmtask.yieldrecord.dao;

import com.ym.agriculture.farmtask.yieldrecord.model.entity.SfStaskYieldRecord;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskAnnualYieldRowVo;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 产量记录 Mapper。
 */
public interface SfStaskYieldRecordMapper
    extends BaseMapperPlus<SfStaskYieldRecord, SfStaskYieldRecord> {

    @Select("""
        SELECT variety_id AS varietyId,
               COALESCE(MAX(NULLIF(TRIM(variety_name_snapshot), '')), CAST(variety_id AS CHAR)) AS varietyName,
               COALESCE(SUM(yield_kg), 0) AS yieldKg
        FROM sf_stask_yield_record
        WHERE tenant_id = #{tenantId}
          AND del_flag = '0'
          AND harvest_date >= #{startDate}
          AND harvest_date <= #{endDate}
        GROUP BY variety_id
        ORDER BY yieldKg DESC, varietyName ASC, variety_id ASC
        """)
    List<SfStaskAnnualYieldRowVo> selectAnnualYield(
        @Param("tenantId") String tenantId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate);
}
