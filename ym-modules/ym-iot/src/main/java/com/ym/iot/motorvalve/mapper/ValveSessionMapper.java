package com.ym.iot.motorvalve.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.motorvalve.domain.ValveSession;
import com.ym.iot.motorvalve.domain.bo.ValveSessionBo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionStatsVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionVo;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 业务历史按记录发生时的租户查询，设备转移不改变历史归属。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface ValveSessionMapper extends BaseMapperPlus<ValveSession, ValveSessionVo> {
    /** 只接收本业务查询参数；租户由已认证的服务层传入。 */
    default QueryWrapper<ValveSession> historyQuery(String tenant, ValveSessionBo bo) {
        if (tenant == null || tenant.isBlank()) throw new ServiceException("历史查询必须指定租户");
        // 保留旧部署可选列兼容性，由实体映射接收实际存在的字段。
        var query = new QueryWrapper<ValveSession>().select("*").eq("tenant_id", tenant);
        if (bo == null) return query;
        query.eq(bo.getDeviceId() != null, "device_id", bo.getDeviceId());
        query.like(
                bo.getDeviceCode() != null && !bo.getDeviceCode().isBlank(),
                "device_code",
                bo.getDeviceCode());
        query.eq(bo.getValveNo() != null, "valve_no", bo.getValveNo());
        query.eq(bo.getStatus() != null, "status", bo.getStatus());
        query.ge(bo.getBeginTime() != null, "open_time", bo.getBeginTime());
        query.le(bo.getEndTime() != null, "open_time", bo.getEndTime());
        return query;
    }

    default List<ValveSessionVo> selectHistory(String tenant, ValveSessionBo bo) {
        return selectVoList(historyQuery(tenant, bo).orderByDesc("open_time", "id"));
    }

    /** 最近结束会话直接在数据库筛选并限量，不加载全部会话。 */
    default List<ValveSessionVo> selectRecentHistory(String tenant, int limit) {
        var query = historyQuery(tenant, null);
        if (limit <= 0) return List.of();
        query.in("status", "CLOSED", "ABORTED")
                .isNotNull("close_time")
                .orderByDesc("close_time", "id");
        IPage<ValveSessionVo> page = selectVoPage(new Page<ValveSession>(1, limit, false), query);
        return page.getRecords();
    }

    default ValveSessionStatsVo selectHistoryStats(String tenant, ValveSessionBo bo) {
        var result = aggregateHistory(historyQuery(tenant, bo));
        long count = result.getTotalSessions();
        long seconds = result.getTotalDurationSeconds();
        result.setTotalDurationMinutes(seconds / 60);
        result.setAvgDurationSeconds(count == 0 ? 0 : seconds / count);
        return result;
    }

    /** 汇总在数据库完成；只统计已结束会话时长，OPEN 单独计数。 */
    @Select(
            """
            SELECT COALESCE(SUM(CASE WHEN status IN ('CLOSED','ABORTED') THEN 1 ELSE 0 END),0) AS total_sessions,
                   COALESCE(SUM(CASE WHEN status IN ('CLOSED','ABORTED') THEN COALESCE(duration_seconds,0) ELSE 0 END),0) AS total_duration_seconds,
                   COALESCE(SUM(CASE WHEN status='OPEN' THEN 1 ELSE 0 END),0) AS open_session_count
            FROM iot_motorvalve_valve_session ${ew.customSqlSegment}
            """)
    ValveSessionStatsVo aggregateHistory(@Param("ew") QueryWrapper<ValveSession> query);
}
