package com.ym.agriculture.farmtask.leaderlabor.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.leaderlabor.model.entity.SfStaskOperationIdempotency;
import org.apache.ibatis.annotations.Mapper;

/** stask 批量操作幂等记录数据访问层。 */
@Mapper
public interface SfStaskOperationIdempotencyMapper extends BaseMapperPlus<SfStaskOperationIdempotency, SfStaskOperationIdempotency> {

    default SfStaskOperationIdempotency selectByScope(String tenantId, Long employeeId, String operationType,
        String idempotencyKey) {
        return selectOne(Wrappers.<SfStaskOperationIdempotency>lambdaQuery()
            .eq(SfStaskOperationIdempotency::getTenantId, tenantId)
            .eq(SfStaskOperationIdempotency::getEmployeeId, employeeId)
            .eq(SfStaskOperationIdempotency::getOperationType, operationType)
            .eq(SfStaskOperationIdempotency::getIdempotencyKey, idempotencyKey));
    }
}
