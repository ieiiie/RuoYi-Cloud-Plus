package com.ym.iot.ownership.service.support;

import com.ym.agriculture.api.farming.RemoteFieldDeviceBindingService;
import com.ym.common.core.exception.ServiceException;
import com.ym.iot.ownership.domain.dto.DeviceOwnershipSnapshot;
import com.ym.iot.ownership.domain.vo.AssignedDeviceVo;
import com.ym.iot.ownership.domain.vo.OwnershipBlockerVo;
import com.ym.iot.ownership.domain.vo.OwnershipHistoryVo;
import com.ym.iot.ownership.domain.vo.UnassignedDeviceVo;
import com.ym.iot.ownership.mapper.DeviceOwnershipMapper;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Repository;

import java.util.*;

/** 归属访问与占用检查的业务门面。数据读写由 Mapper 执行， 保留未分配、冻结、共享锁及跨业务占用的判断，不承担归属变更。 */
@Repository
public class DeviceOwnershipRepository {
    private final DeviceOwnershipMapper ownershipMapper;

    @DubboReference(
            group = "iot-ownership-guard",
            version = "1.0.0",
            check = false,
            retries = 0,
            timeout = 5000)
    private RemoteFieldDeviceBindingService fieldBindings;

    public DeviceOwnershipRepository(DeviceOwnershipMapper ownershipMapper) {
        this.ownershipMapper = ownershipMapper;
    }

    public boolean deviceExists(Long id) {
        return ownershipMapper.countDevice(id) == 1;
    }

    public DeviceOwnershipSnapshot find(Long id) {
        var rows = ownershipMapper.selectSnapshot(id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public DeviceOwnershipSnapshot lock(Long id) {
        // Profile lock also serializes two first assignments when there is no ownership row yet.
        var devices = ownershipMapper.lockExistingDevice(id);
        if (devices.isEmpty()) throw new ServiceException("设备不存在或已删除");
        var rows = ownershipMapper.lockSnapshot(id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public List<Long> accessible(String tenantId, Collection<Long> ids) {
        if (ids != null && ids.isEmpty()) return List.of();
        return ownershipMapper.selectAccessible(tenantId, ids);
    }

    public List<AssignedDeviceVo> assigned(long afterId, int limit, String tenantId) {
        return ownershipMapper.selectAssigned(afterId, limit, tenantId);
    }

    public List<UnassignedDeviceVo> pool(long afterId, int limit) {
        return ownershipMapper.selectUnassigned(afterId, limit);
    }

    public List<OwnershipHistoryVo> history(Long id, long afterVersion, int limit) {
        return ownershipMapper.selectHistory(id, afterVersion, limit);
    }

    public List<OwnershipBlockerVo> blockers(Long id) {
        List<OwnershipBlockerVo> result = new ArrayList<>();
        addBlocker(result, "FIELD_BINDING", countFieldBindings(id), "请先由原租户解除全部地块绑定");
        addBlocker(
                result, "VALVE_OPEN", ownershipMapper.countOpenValves(id), "请先关闭阀门并确认所有 OPEN 会话结束");
        addBlocker(
                result,
                "VALVE_PENDING",
                ownershipMapper.countPendingValveCommands(id),
                "请先等待阀门待确认指令完成或人工核实终止");
        addBlocker(
                result,
                "FERTILIZER_PENDING",
                ownershipMapper.countPendingFertilizerCommands(id),
                "请先结束施肥任务并核实待完成的控制日志");
        addBlocker(
                result,
                "JETLINKS_PENDING",
                ownershipMapper.countPendingCoreCommands(id),
                "请先确认全部 JetLinks 待完成指令，包括 UNKNOWN 状态；未知结果不能视为完成");
        return result;
    }

    private long countFieldBindings(Long id) {
        String code = ownershipMapper.selectDeviceCode(id);
        if (code == null || code.isBlank()) throw new ServiceException("设备编号缺失，无法核实地块绑定");
        try {
            long count = fieldBindings.countActiveBindings(code);
            if (count < 0) throw new ServiceException("农业服务返回无效绑定数量");
            return count;
        } catch (RuntimeException failure) {
            throw new ServiceException("农业地块绑定检查失败，请待农业服务恢复后重试");
        }
    }

    private static void addBlocker(
            List<OwnershipBlockerVo> result, String code, long count, String message) {
        if (count > 0) result.add(new OwnershipBlockerVo(code, count, message));
    }
}
