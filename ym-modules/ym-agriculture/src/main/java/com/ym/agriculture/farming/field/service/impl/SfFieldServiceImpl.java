package com.ym.agriculture.farming.field.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.bo.SfPlantingBatchBo;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.agriculture.farming.field.dao.SfFieldIotMapper;
import com.ym.agriculture.farming.field.layout.service.IGreenhouseLayoutService;
import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.model.entity.SfFieldIot;
import com.ym.agriculture.farming.field.model.vo.SfFieldExportVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldBatchInfoVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldDetailVo;
import com.ym.agriculture.farming.field.service.ISfFieldService;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import javax.sql.DataSource;
import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 地块档案服务实现。
 *
 * @author ym-cloud
 */
@Service
@RequiredArgsConstructor
public class SfFieldServiceImpl implements ISfFieldService {

    private static final String NORMAL = "0";
    private static final String DEFAULT_FIELD_TYPE = "FIELD";
    private static final String DEFAULT_FIELD_STATUS = "IDLE";
    private static final String DEFAULT_MAP_PROVIDER = "tianditu";

    private final SfFieldMapper fieldMapper;
    private final SfFieldIotMapper fieldIotMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final ISfPlantingBatchService plantingBatchService;
    private final SfCropVarietyMapper cropVarietyMapper;
    private final IGreenhouseLayoutService greenhouseLayoutService;

    @DubboReference
    private RemoteIotDeviceService remoteIotDeviceService;

    @Autowired
    private DataSource ownershipBindingDataSource;

    @Value("${ym.iot.ownership.iot-schema:ym-iot}")
    private String ownershipIotSchema = "ym-iot";

    @Override
    public List<SfFieldVo> queryList(SfFieldBo bo) {
        return listFieldsOrdered(bo);
    }

    @Override
    public List<SfFieldVo> listFieldsOrdered(SfFieldBo bo) {
        return enrich(fieldMapper.selectFieldList(bo));
    }

    @Override
    public PageResult<SfFieldVo> queryPageList(SfFieldBo bo, PageQuery pageQuery) {
        Page<SfFieldVo> page = fieldMapper.selectFieldPage(pageQuery.build(), bo);
        return PageResult.build(enrich(page.getRecords()), page.getTotal());
    }

    @Override
    public SfFieldVo queryById(Long fieldId) {
        SfFieldVo field = fieldMapper.selectVoById(fieldId);
        if (field == null) {
            throw new ServiceException("地块不存在");
        }
        enrich(field);
        return field;
    }

    @Override
    public boolean checkFieldNameUnique(SfFieldBo bo) {
        return !fieldMapper.existsByFieldName(normalize(bo.getFieldName()), bo.getFieldId());
    }

    @Override
    public SfFieldDetailVo queryDetailById(Long fieldId) {
        SfFieldVo field = queryById(fieldId);
        SfFieldDetailVo detail = new SfFieldDetailVo();
        BeanUtil.copyProperties(field, detail);
        SfPlantingBatchBo query = new SfPlantingBatchBo();
        query.setFieldId(fieldId);
        List<SfPlantingBatchVo> batches = plantingBatchService.queryList(query);
        detail.setPlantingBatches(batches);
        detail.setBatchCount((long) batches.size());
        detail.setActiveBatch(queryBatchInfoByFieldId(fieldId));
        List<String> deviceSns = listIotDeviceSnsByField(fieldId);
        detail.setIotDevices(deviceSns.isEmpty() ? List.of() : remoteIotDeviceService.listDevicesByCodes(deviceSns));
        return detail;
    }

    @Override
    public SfFieldBatchInfoVo queryBatchInfoByFieldId(Long fieldId) {
        SfField existing = fieldMapper.selectById(fieldId);
        if (existing == null || !NORMAL.equals(existing.getDelFlag())) {
            throw new ServiceException("地块不存在");
        }
        Long batchId = plantingBatchMapper.selectActiveBatchIdByFieldId(fieldId);
        SfFieldBatchInfoVo vo = new SfFieldBatchInfoVo();
        vo.setHasActiveBatch(batchId != null);
        if (batchId == null) {
            return vo;
        }
        SfPlantingBatchVo batch = plantingBatchService.queryById(batchId);
        vo.setBatchId(batch.getBatchId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setBatchStatus(batch.getBatchStatus());
        vo.setVarietyId(batch.getVarietyId());
        vo.setVarietyName(batch.getVarietyName());
        vo.setSpeciesName(batch.getSpeciesName());
        vo.setSpeciesImageUrl(batch.getSpeciesImageUrl());
        vo.setSowingDate(batch.getSowingDate());
        if (batch.getVarietyId() != null) {
            SfCropVarietyVo variety = cropVarietyMapper.selectVoById(batch.getVarietyId());
            if (variety != null) {
                vo.setSpeciesId(variety.getSpeciesId());
                if (StringUtils.isBlank(vo.getSpeciesName())) {
                    vo.setSpeciesName(variety.getSpeciesName());
                }
            }
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertByBo(SfFieldBo bo) {
        normalizeAndValidate(bo);
        SfField field = MapstructUtils.convert(bo, SfField.class);
        if (field.getOwnerUserId() == null) {
            field.setOwnerUserId(LoginHelper.getUserId());
        }
        if (field.getSortOrder() == null) {
            field.setSortOrder(0);
        }
        if (StringUtils.isBlank(field.getFieldType())) {
            field.setFieldType(DEFAULT_FIELD_TYPE);
        }
        if (StringUtils.isBlank(field.getStatus())) {
            field.setStatus(NORMAL);
        }
        if (StringUtils.isBlank(field.getFieldStatus())) {
            field.setFieldStatus(DEFAULT_FIELD_STATUS);
        }
        if (StringUtils.isBlank(field.getMapProvider())) {
            field.setMapProvider(DEFAULT_MAP_PROVIDER);
        }
        boolean result = fieldMapper.insert(field) > 0;
        if (result) {
            bo.setFieldId(field.getFieldId());
            if (FieldType.GREENHOUSE.equals(FieldType.normalize(field.getFieldType()))) {
                greenhouseLayoutService.saveLayout(bo.getGreenhouseLayoutDraft(), field.getFieldId());
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(SfFieldBo bo) {
        SfField existing = fieldMapper.selectById(bo.getFieldId());
        if (existing == null) {
            throw new ServiceException("地块不存在");
        }
        normalizeAndValidate(bo);
        SfField update = MapstructUtils.convert(bo, SfField.class);
        boolean result = fieldMapper.updateById(update) > 0;
        if (result) {
            if (FieldType.GREENHOUSE.equals(FieldType.normalize(bo.getFieldType()))) {
                greenhouseLayoutService.saveLayout(bo.getGreenhouseLayoutDraft(), bo.getFieldId());
            } else if (FieldType.GREENHOUSE.equals(FieldType.normalize(existing.getFieldType()))) {
                greenhouseLayoutService.removeFields(List.of(bo.getFieldId()));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateStatus(Long fieldId, String status) {
        if (fieldMapper.selectById(fieldId) == null) {
            throw new ServiceException("地块不存在");
        }
        return fieldMapper.updateStatus(fieldId, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteWithValidByIds(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return false;
        }
        boolean result = fieldMapper.deleteBatchIds(fieldIds) > 0;
        if (result) {
            fieldIotMapper.deleteByFieldIds(LoginHelper.getTenantId(), fieldIds);
            greenhouseLayoutService.removeFields(fieldIds);
        }
        return result;
    }

    @Override
    public List<SfFieldExportVo> queryExportList(SfFieldBo bo) {
        return BeanUtil.copyToList(queryList(bo), SfFieldExportVo.class);
    }

    @Override
    public List<String> listIotDeviceSnsByField(Long fieldId) {
        requireField(fieldId);
        return fieldIotMapper.selectNormalListByFieldId(fieldId).stream()
            .map(SfFieldIot::getDeviceSn)
            .filter(StringUtils::isNotBlank)
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveIotDeviceSnsForField(Long fieldId, List<String> deviceSns) {
        SfField field = requireField(fieldId);
        List<String> normalized = normalizeDeviceSns(deviceSns);
        requireDeviceOwnershipForBinding(field, normalized);
        validateDevices(fieldId, normalized);
        fieldIotMapper.deleteByFieldId(field.getTenantId(), fieldId);
        normalized.forEach(deviceSn -> insertBinding(field, deviceSn));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addIotDeviceSnForField(Long fieldId, String deviceSn) {
        SfField field = requireField(fieldId);
        List<String> normalized = deviceSn == null ? List.of() : normalizeDeviceSns(List.of(deviceSn));
        if (normalized.isEmpty()) {
            throw new ServiceException("设备编码不能为空");
        }
        String normalizedSn = normalized.getFirst();
        requireDeviceOwnershipForBinding(field, normalized);
        if (fieldIotMapper.existsByFieldIdAndDeviceSn(fieldId, normalizedSn)) {
            return;
        }
        validateDevices(fieldId, normalized);
        insertBinding(field, normalizedSn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeIotDeviceSnForField(Long fieldId, String deviceSn) {
        SfField field = requireField(fieldId);
        List<String> normalized = deviceSn == null ? List.of() : normalizeDeviceSns(List.of(deviceSn));
        if (!normalized.isEmpty()) {
            fieldIotMapper.deleteByFieldIdAndDeviceSn(field.getTenantId(), fieldId, normalized.getFirst());
        }
    }

    private SfField requireField(Long fieldId) {
        SfField field = fieldMapper.selectById(fieldId);
        if (field == null || !NORMAL.equals(field.getDelFlag())) {
            throw new ServiceException("地块不存在");
        }
        return field;
    }

    private List<String> normalizeDeviceSns(Collection<String> deviceSns) {
        if (deviceSns == null || deviceSns.isEmpty()) {
            return List.of();
        }
        return deviceSns.stream()
            .filter(StringUtils::isNotBlank)
            .map(String::trim)
            .distinct()
            .toList();
    }

    private void validateDevices(Long fieldId, List<String> deviceSns) {
        if (deviceSns.isEmpty()) {
            return;
        }
        Set<String> existingCodes = remoteIotDeviceService.listDevicesByCodes(deviceSns).stream()
            .map(RemoteDeviceSummaryVo::getDeviceCode)
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toSet());
        List<String> missing = deviceSns.stream()
            .filter(deviceSn -> !existingCodes.contains(deviceSn))
            .toList();
        if (!missing.isEmpty()) {
            throw new ServiceException("物联网设备不存在或不可用：" + String.join("、", missing));
        }
        for (String deviceSn : deviceSns) {
            Long boundFieldId = fieldIotMapper.selectFieldIdByDeviceSn(deviceSn);
            if (boundFieldId != null && !boundFieldId.equals(fieldId)) {
                throw new ServiceException("设备 " + deviceSn + " 已绑定其他地块");
            }
        }
    }

    /**
     * The agriculture transaction locks the same ownership row used by IoT transfers until binding commit.
     * Both schemas must be on this MySQL server. Never switch @DS inside an existing transaction.
     * No profile/binding/default-tenant fallback: a missing ownership table or row refuses the bind.
     */
    private void requireDeviceOwnershipForBinding(SfField field, List<String> deviceSns) {
        String tenantId = TenantHelper.getTenantId();
        if (!LoginHelper.isLogin() || StringUtils.isBlank(tenantId) || !tenantId.equals(field.getTenantId())) {
            throw new ServiceException("只能在当前登录租户的地块绑定设备");
        }
        if (deviceSns.isEmpty()) return;
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new ServiceException("设备绑定必须在事务中校验归属");
        }
        if (!ownershipIotSchema.matches("[A-Za-z0-9_-]+")) {
            throw new ServiceException("设备归属库配置无效");
        }
        JdbcTemplate jdbc = new JdbcTemplate(ownershipBindingDataSource);
        String schema = "`" + ownershipIotSchema + "`";
        // Stable lock order for multi-device replacement. Do not deduce ownership from duplicate codes.
        for (String sn : deviceSns.stream().sorted().toList()) {
            var rows = jdbc.queryForList("SELECT o.tenant_id,o.fence_status,o.assignment_version FROM "
                + schema + ".iot_device d JOIN " + schema + ".iot_device_ownership o ON o.device_id=d.device_id "
                + "WHERE d.device_code=? AND d.del_flag='0' ORDER BY d.device_id FOR UPDATE", sn);
            if (rows.size() != 1 || !tenantId.equals(rows.getFirst().get("tenant_id"))
                || !"ACTIVE".equals(rows.getFirst().get("fence_status"))) {
                throw new ServiceException("设备 " + sn + " 未分配、归属变更中、编码不唯一或不属于当前租户");
            }
        }
    }

    private void insertBinding(SfField field, String deviceSn) {
        SfFieldIot binding = new SfFieldIot();
        binding.setTenantId(field.getTenantId());
        binding.setFieldId(field.getFieldId());
        binding.setDeviceSn(deviceSn);
        binding.setDelFlag(NORMAL);
        fieldIotMapper.insert(binding);
    }

    private void normalizeAndValidate(SfFieldBo bo) {
        bo.setFieldCode(normalize(bo.getFieldCode()));
        bo.setFieldName(normalize(bo.getFieldName()));
        bo.setGreenhouseShortName(normalize(bo.getGreenhouseShortName()));
        bo.setGreenhouseColor(normalize(bo.getGreenhouseColor()));
        bo.setMapProvider(normalize(bo.getMapProvider()));
        bo.setFieldType(FieldType.normalize(bo.getFieldType()));
        if (StringUtils.isNotBlank(bo.getFieldCode())
            && fieldMapper.existsByFieldCode(bo.getFieldCode(), bo.getFieldId())) {
            throw new ServiceException("地块编码已存在");
        }
        if (fieldMapper.existsByFieldName(bo.getFieldName(), bo.getFieldId())) {
            throw new ServiceException("地块名称已存在");
        }
        if (StringUtils.isNotBlank(bo.getGreenhouseColor())) {
            bo.setGreenhouseColor(bo.getGreenhouseColor().toUpperCase(Locale.ROOT));
        }
    }

    private static String normalize(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private static List<SfFieldVo> enrich(List<SfFieldVo> fields) {
        fields.forEach(SfFieldServiceImpl::enrich);
        return fields;
    }

    private static void enrich(SfFieldVo field) {
        if (!NORMAL.equals(field.getStatus())) {
            field.setMapDisplayStatus("DISABLED");
            field.setFieldStatusDisplay("停用");
            return;
        }
        String fieldStatus = StringUtils.defaultIfBlank(field.getFieldStatus(), DEFAULT_FIELD_STATUS);
        field.setMapDisplayStatus(fieldStatus);
        field.setFieldStatusDisplay(switch (fieldStatus) {
            case "IN_USE" -> "使用中";
            case "MAINTENANCE" -> "维护中";
            default -> "空闲";
        });
    }
}
