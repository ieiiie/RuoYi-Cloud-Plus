package com.ym.iot.fertilizer.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.fertilizer.domain.IotFertilizerRecord;
import com.ym.iot.fertilizer.domain.bo.IotFertilizerRecordBo;
import com.ym.iot.fertilizer.domain.vo.IotFertilizerRecordVo;

/** 业务历史按记录发生时的租户查询，设备转移不改变历史归属。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface IotFertilizerRecordMapper
        extends BaseMapperPlus<IotFertilizerRecord, IotFertilizerRecordVo> {
    /** 只接收本业务查询参数；租户由已认证的服务层传入。 */
    default QueryWrapper<IotFertilizerRecord> historyQuery(
            String tenant, IotFertilizerRecordBo bo) {
        if (tenant == null || tenant.isBlank()) throw new ServiceException("历史查询必须指定租户");
        // 保留旧部署可选列兼容性，由实体映射接收实际存在的字段。
        var query = new QueryWrapper<IotFertilizerRecord>().select("*").eq("tenant_id", tenant);
        if (bo == null) return query;
        query.eq(bo.getDeviceId() != null, "device_id", bo.getDeviceId());
        query.eq(
                bo.getDeviceCode() != null && !bo.getDeviceCode().isBlank(),
                "device_code",
                bo.getDeviceCode());
        query.eq(
                bo.getFertilizationType() != null, "fertilization_type", bo.getFertilizationType());
        query.ge(bo.getBeginTime() != null, "record_time", bo.getBeginTime());
        query.le(bo.getEndTime() != null, "record_time", bo.getEndTime());
        return query;
    }

    /** 由分页插件执行 COUNT 与分页 SQL，只读取当前页。 */
    default IPage<IotFertilizerRecordVo> selectHistoryPage(
            String tenant, IotFertilizerRecordBo bo, Page<IotFertilizerRecord> page) {
        return selectVoPage(page, historyQuery(tenant, bo).orderByDesc("record_time", "record_id"));
    }

    default IotFertilizerRecordVo selectHistoryById(String tenant, Long recordId) {
        var query = historyQuery(tenant, null);
        if (recordId == null) return null;
        return selectVoOne(query.eq("record_id", recordId));
    }
}
