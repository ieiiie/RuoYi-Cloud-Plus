package com.ym.agriculture.farmtask.inventory.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetDeviceMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetTypeMapper;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.AssetUsageLogMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetDevice;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetType;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.AssetUsageLog;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetActionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceBatchBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTimelineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTypeSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTypeVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.service.IAssetInventoryService;
import com.ym.agriculture.farmtask.inventory.service.InventoryInfrastructure;
import com.ym.agriculture.farmtask.inventory.support.AssetStateTransitions;
import com.ym.agriculture.farmtask.inventory.support.InventoryI18nResourceRegistrar;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssetInventoryServiceImpl implements IAssetInventoryService {

    private final InventoryInfrastructure infrastructure;
    private final AssetTypeMapper typeMapper;
    private final AssetDeviceMapper deviceMapper;
    private final AssetUsageLogMapper usageLogMapper;
    private final InventoryI18nResourceRegistrar i18nRegistrar;

    @Override
    public List<AssetTypeVo> typeSummary() {
        List<AssetType> types = typeMapper.selectList(Wrappers.<AssetType>lambdaQuery()
            .eq(AssetType::getTenantId, infrastructure.tenantId()).eq(AssetType::getDelFlag, "0")
            .orderByAsc(AssetType::getTypeName).orderByAsc(AssetType::getAssetTypeId));
        if (types.isEmpty()) {
            return List.of();
        }
        Map<Long, List<AssetDevice>> devices = deviceMapper.selectList(Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, infrastructure.tenantId()).eq(AssetDevice::getDelFlag, "0")
            .in(AssetDevice::getAssetTypeId, types.stream().map(AssetType::getAssetTypeId).toList()))
            .stream().collect(Collectors.groupingBy(AssetDevice::getAssetTypeId));
        return types.stream().map(type -> toTypeVo(type, devices.getOrDefault(type.getAssetTypeId(), List.of()))).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo addType(AssetTypeSaveBo bo) {
        Date now = new Date();
        AssetType type = new AssetType();
        type.setAssetTypeId(IdWorker.getId());
        type.setTenantId(infrastructure.tenantId());
        copyType(type, bo);
        type.setVersion(0L);
        type.setDelFlag("0");
        type.setCreateBy(infrastructure.operatorId());
        type.setCreateTime(now);
        type.setUpdateBy(infrastructure.operatorId());
        type.setUpdateTime(now);
        try {
            typeMapper.insert(type);
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("资产种类编码已存在");
        }
        infrastructure.audit("ASSET_TYPE", type.getAssetTypeId(), "CREATE", bo.getRemark(), null, type);
        i18nRegistrar.registerAssetTypes(type.getTenantId(), List.of(type.getAssetTypeId()));
        return new MutationVo(type.getAssetTypeId(), type.getVersion(), Boolean.TRUE.equals(type.getEnabled()) ? "ENABLED" : "DISABLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo updateType(Long id, AssetTypeSaveBo bo) {
        AssetType type = requireType(id);
        requireVersion(type.getVersion(), bo.getVersion());
        AssetType before = BeanUtil.copyProperties(type, AssetType.class);
        copyType(type, bo);
        type.setUpdateBy(infrastructure.operatorId());
        type.setUpdateTime(new Date());
        try {
            if (typeMapper.updateById(type) != 1) {
                throw versionConflict();
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("资产种类编码已存在");
        }
        infrastructure.audit("ASSET_TYPE", id, "UPDATE", bo.getRemark(), before, type);
        i18nRegistrar.registerAssetTypes(type.getTenantId(), List.of(id));
        return new MutationVo(id, type.getVersion(), Boolean.TRUE.equals(type.getEnabled()) ? "ENABLED" : "DISABLED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeType(Long id, Long version) {
        if (!LoginHelper.isSuperAdmin()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403,
                "只有超级管理员可以删除资产种类");
        }
        AssetType type = typeMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (type == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "资产种类不存在");
        }
        requireVersion(type.getVersion(), version);
        long deviceCount = deviceMapper.selectCount(Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, infrastructure.tenantId())
            .eq(AssetDevice::getAssetTypeId, id));
        if (deviceCount > 0) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ASSET_STATE_INVALID,
                "资产种类已存在设备或历史记录，不能删除");
        }
        AssetType before = BeanUtil.copyProperties(type, AssetType.class);
        type.setDelFlag("1");
        type.setUpdateBy(infrastructure.operatorId());
        type.setUpdateTime(new Date());
        if (typeMapper.updateById(type) != 1) {
            throw versionConflict();
        }
        infrastructure.audit("ASSET_TYPE", id, "DELETE", null, before, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<MutationVo> batchAddDevices(AssetDeviceBatchBo bo) {
        AssetType type = requireType(bo.getAssetTypeId());
        if (!Boolean.TRUE.equals(type.getEnabled())) {
            throw InventoryBusinessException.rule("资产种类已停用");
        }
        if (bo.getQuantity() == null || bo.getQuantity() < 1 || bo.getQuantity() > 100) {
            throw InventoryBusinessException.rule("单次批量新增数量必须在1到100之间");
        }
        Date now = new Date();
        List<MutationVo> result = new ArrayList<>();
        try {
            for (int index = 1; index <= bo.getQuantity(); index++) {
                AssetDevice device = new AssetDevice();
                device.setAssetDeviceId(IdWorker.getId());
                device.setTenantId(infrastructure.tenantId());
                device.setAssetTypeId(type.getAssetTypeId());
                device.setDeviceNo(StrUtil.trim(bo.getDeviceNoPrefix()) + StrUtil.padPre(String.valueOf(index), 4, '0'));
                device.setDeviceName(StrUtil.trim(bo.getDeviceName()));
                device.setStatus(InventoryConstants.ASSET_IDLE);
                device.setAcquiredDate(bo.getAcquiredDate());
                device.setVersion(0L);
                device.setDelFlag("0");
                device.setCreateBy(infrastructure.operatorId());
                device.setCreateTime(now);
                device.setUpdateBy(infrastructure.operatorId());
                device.setUpdateTime(now);
                device.setRemark(bo.getRemark());
                deviceMapper.insert(device);
                addTimeline(device, "CREATE", null, InventoryConstants.ASSET_IDLE, null, null, bo.getRemark());
                result.add(new MutationVo(device.getAssetDeviceId(), device.getVersion(), device.getStatus()));
            }
        } catch (DuplicateKeyException exception) {
            throw InventoryBusinessException.rule("批量设备编号与现有资产重复，整批未保存");
        }
        infrastructure.audit("ASSET_TYPE", type.getAssetTypeId(), "BATCH_CREATE_DEVICE", bo.getRemark(), null, result);
        i18nRegistrar.registerAssetDevices(type.getTenantId(),
            result.stream().map(MutationVo::getId).toList());
        return result;
    }

    @Override
    public PageResult<AssetDeviceVo> pageDevices(AssetQuery query, PageQuery pageQuery) {
        Page<AssetDevice> page = deviceMapper.selectPage(pageQuery.build(), Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, infrastructure.tenantId()).eq(AssetDevice::getDelFlag, "0")
            .eq(query.getAssetTypeId() != null, AssetDevice::getAssetTypeId, query.getAssetTypeId())
            .eq(StrUtil.isNotBlank(query.getStatus()), AssetDevice::getStatus, query.getStatus())
            .and(StrUtil.isNotBlank(query.getKeyword()), wrapper -> wrapper.like(AssetDevice::getDeviceNo, query.getKeyword())
                .or().like(AssetDevice::getDeviceName, query.getKeyword())
                .or().like(AssetDevice::getCurrentHolderNameSnapshot, query.getKeyword()))
            .orderByDesc(AssetDevice::getUpdateTime).orderByDesc(AssetDevice::getAssetDeviceId));
        Map<Long, AssetType> typeById = page.getRecords().isEmpty() ? Map.of()
            : typeMapper.selectList(Wrappers.<AssetType>lambdaQuery()
                .eq(AssetType::getTenantId, infrastructure.tenantId())
                .in(AssetType::getAssetTypeId, page.getRecords().stream().map(AssetDevice::getAssetTypeId).distinct().toList()))
                .stream().collect(Collectors.toMap(AssetType::getAssetTypeId, Function.identity()));
        Page<AssetDeviceVo> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(item -> toDeviceVo(item, typeById.get(item.getAssetTypeId()), false)).toList());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public AssetDeviceVo getDevice(Long id) {
        AssetDevice device = requireDevice(id);
        return toDeviceVo(device, requireType(device.getAssetTypeId()), true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVo transition(Long id, String action, AssetActionBo bo) {
        AssetDevice device = deviceMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (device == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "资产设备不存在");
        }
        requireVersion(device.getVersion(), bo.getVersion());
        String from = device.getStatus();
        String normalizedAction = StrUtil.trim(action).toUpperCase(Locale.ROOT);
        String to;
        switch (normalizedAction) {
            case "CHECK_OUT" -> {
                to = AssetStateTransitions.checkOut(from);
                if (bo.getHolderEmployeeId() == null || StrUtil.isBlank(bo.getHolderName())) {
                    throw InventoryBusinessException.rule("领用人不能为空");
                }
                device.setCurrentHolderEmployeeId(bo.getHolderEmployeeId());
                device.setCurrentHolderNameSnapshot(StrUtil.trim(bo.getHolderName()));
            }
            case "RETURN" -> to = AssetStateTransitions.returnDevice(from);
            case "CHANGE_STATUS" -> to = AssetStateTransitions.changeStatus(from, bo.getTargetStatus());
            default -> throw InventoryBusinessException.rule("不支持的资产操作");
        }
        String holderName = device.getCurrentHolderNameSnapshot();
        Long holderId = device.getCurrentHolderEmployeeId();
        device.setStatus(to);
        if (!InventoryConstants.ASSET_IN_USE.equals(to)) {
            device.setCurrentHolderEmployeeId(null);
            device.setCurrentHolderNameSnapshot(null);
        }
        device.setUpdateBy(infrastructure.operatorId());
        device.setUpdateTime(new Date());
        if (deviceMapper.updateById(device) != 1) {
            throw versionConflict();
        }
        addTimeline(device, normalizedAction, from, to, holderId, holderName, bo.getRemark());
        infrastructure.audit("ASSET_DEVICE", id, normalizedAction, bo.getRemark(), from, device);
        i18nRegistrar.registerAssetDevices(device.getTenantId(), List.of(id));
        return new MutationVo(id, device.getVersion(), device.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeDevice(Long id, Long version) {
        if (!LoginHelper.isSuperAdmin()) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403,
                "只有超级管理员可以删除资产设备");
        }
        AssetDevice device = deviceMapper.selectForUpdate(infrastructure.tenantId(), id);
        if (device == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "资产设备不存在");
        }
        requireVersion(device.getVersion(), version);
        if (InventoryConstants.ASSET_IN_USE.equals(device.getStatus())) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ASSET_STATE_INVALID,
                "在用设备必须先归还才能删除");
        }
        long history = usageLogMapper.selectCount(Wrappers.<AssetUsageLog>lambdaQuery()
            .eq(AssetUsageLog::getTenantId, infrastructure.tenantId())
            .eq(AssetUsageLog::getAssetDeviceId, id));
        if (history > 1) {
            throw InventoryBusinessException.rule("资产已有领用或状态历史，不能删除");
        }
        AssetDevice before = BeanUtil.copyProperties(device, AssetDevice.class);
        device.setDelFlag("1");
        device.setUpdateBy(infrastructure.operatorId());
        device.setUpdateTime(new Date());
        if (deviceMapper.updateById(device) != 1) {
            throw versionConflict();
        }
        infrastructure.audit("ASSET_DEVICE", id, "DELETE", null, before, null);
    }

    @Override
    public long assetCount() {
        return deviceMapper.selectCount(Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, infrastructure.tenantId()).eq(AssetDevice::getDelFlag, "0"));
    }

    private void addTimeline(AssetDevice device, String action, String from, String to,
                             Long holderId, String holderName, String remark) {
        AssetUsageLog log = new AssetUsageLog();
        log.setUsageLogId(IdWorker.getId());
        log.setTenantId(infrastructure.tenantId());
        log.setAssetDeviceId(device.getAssetDeviceId());
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setHolderEmployeeId(holderId);
        log.setHolderNameSnapshot(holderName);
        log.setOperatorEmployeeId(infrastructure.operatorId());
        log.setOperatorNameSnapshot(infrastructure.operatorName());
        log.setOccurredAt(new Date());
        log.setRemark(remark);
        log.setCreateTime(new Date());
        usageLogMapper.insert(log);
    }

    private AssetType requireType(Long id) {
        AssetType type = typeMapper.selectOne(Wrappers.<AssetType>lambdaQuery()
            .eq(AssetType::getTenantId, infrastructure.tenantId()).eq(AssetType::getAssetTypeId, id)
            .eq(AssetType::getDelFlag, "0"));
        if (type == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "资产种类不存在");
        }
        return type;
    }

    private AssetDevice requireDevice(Long id) {
        AssetDevice device = deviceMapper.selectOne(Wrappers.<AssetDevice>lambdaQuery()
            .eq(AssetDevice::getTenantId, infrastructure.tenantId()).eq(AssetDevice::getAssetDeviceId, id)
            .eq(AssetDevice::getDelFlag, "0"));
        if (device == null) {
            throw new InventoryBusinessException(InventoryConstants.ERROR_ORDER_NOT_FOUND, 404, "资产设备不存在");
        }
        return device;
    }

    private AssetTypeVo toTypeVo(AssetType type, List<AssetDevice> devices) {
        AssetTypeVo vo = new AssetTypeVo();
        vo.setId(type.getAssetTypeId());
        vo.setTypeCode(type.getTypeCode());
        vo.setTypeName(type.getTypeName());
        vo.setEnabled(type.getEnabled());
        vo.setVersion(type.getVersion());
        vo.setTotalCount(devices.size());
        vo.setIdleCount(countStatus(devices, InventoryConstants.ASSET_IDLE));
        vo.setInUseCount(countStatus(devices, InventoryConstants.ASSET_IN_USE));
        vo.setMaintenanceCount(countStatus(devices, InventoryConstants.ASSET_MAINTENANCE));
        vo.setScrappedCount(countStatus(devices, InventoryConstants.ASSET_SCRAPPED));
        return vo;
    }

    private AssetDeviceVo toDeviceVo(AssetDevice device, AssetType type, boolean detail) {
        AssetDeviceVo vo = new AssetDeviceVo();
        vo.setId(device.getAssetDeviceId());
        vo.setAssetTypeId(device.getAssetTypeId());
        vo.setAssetTypeName(type == null ? null : type.getTypeName());
        vo.setDeviceNo(device.getDeviceNo());
        vo.setDeviceName(device.getDeviceName());
        vo.setStatus(device.getStatus());
        vo.setCurrentHolderEmployeeId(device.getCurrentHolderEmployeeId());
        vo.setCurrentHolderName(device.getCurrentHolderNameSnapshot());
        vo.setAcquiredDate(device.getAcquiredDate());
        vo.setVersion(device.getVersion());
        vo.setRemark(device.getRemark());
        if (detail) {
            vo.setTimeline(usageLogMapper.selectList(Wrappers.<AssetUsageLog>lambdaQuery()
                .eq(AssetUsageLog::getTenantId, infrastructure.tenantId())
                .eq(AssetUsageLog::getAssetDeviceId, device.getAssetDeviceId())
                .orderByDesc(AssetUsageLog::getOccurredAt).orderByDesc(AssetUsageLog::getUsageLogId))
                .stream().map(this::toTimelineVo).toList());
        }
        return vo;
    }

    private AssetTimelineVo toTimelineVo(AssetUsageLog log) {
        AssetTimelineVo vo = new AssetTimelineVo();
        vo.setId(log.getUsageLogId());
        vo.setAction(log.getAction());
        vo.setFromStatus(log.getFromStatus());
        vo.setToStatus(log.getToStatus());
        vo.setHolderEmployeeId(log.getHolderEmployeeId());
        vo.setHolderName(log.getHolderNameSnapshot());
        vo.setOperatorName(log.getOperatorNameSnapshot());
        vo.setOccurredAt(log.getOccurredAt());
        vo.setRemark(log.getRemark());
        return vo;
    }

    private long countStatus(List<AssetDevice> devices, String status) {
        return devices.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private void copyType(AssetType type, AssetTypeSaveBo bo) {
        type.setTypeCode(StrUtil.trim(bo.getTypeCode()));
        type.setTypeName(StrUtil.trim(bo.getTypeName()));
        type.setEnabled(!Boolean.FALSE.equals(bo.getEnabled()));
        type.setRemark(StrUtil.trim(bo.getRemark()));
    }

    private void requireVersion(Long current, Long request) {
        if (request == null || !request.equals(current)) {
            throw versionConflict();
        }
    }

    private InventoryBusinessException versionConflict() {
        return new InventoryBusinessException(InventoryConstants.ERROR_VERSION_CONFLICT,
            "数据版本已变化，请刷新后重试");
    }

}
