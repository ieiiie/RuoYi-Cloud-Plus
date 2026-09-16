package com.ym.agriculture.farming.farmrecord.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.farmrecord.dao.SfFarmingRecordTypeMapper;
import com.ym.agriculture.farming.farmrecord.model.bo.SfFarmingRecordTypeSaveBo;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecordType;
import com.ym.agriculture.farming.farmrecord.model.vo.SfFarmingRecordTypeVo;
import com.ym.agriculture.farming.farmrecord.service.ISfFarmingRecordTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 遗留农事类型服务。
 * <p>
 * 新移动端农事记录已改用 {@code sf_farm_work_dict}，本服务仅保留给旧后台维护入口编译和过渡使用。
 * </p>
 *
 * @author ym-cloud
 */
@RequiredArgsConstructor
@Service
public class SfFarmingRecordTypeServiceImpl implements ISfFarmingRecordTypeService {

    private final SfFarmingRecordTypeMapper typeMapper;

    @Override
    public List<SfFarmingRecordTypeVo> listEnabled() {
        return typeMapper.selectEnabledSorted(requireTenantId()).stream()
            .map(this::toVo)
            .toList();
    }

    @Override
    public List<SfFarmingRecordTypeVo> listAllNormal() {
        String tenantId = requireTenantId();
        return typeMapper.selectList(Wrappers.<SfFarmingRecordType>lambdaQuery()
                .eq(SfFarmingRecordType::getTenantId, tenantId)
                .eq(SfFarmingRecordType::getDelFlag, SystemConstants.NORMAL)
                .orderByAsc(SfFarmingRecordType::getSortOrder)
                .orderByAsc(SfFarmingRecordType::getTypeId))
            .stream()
            .map(this::toVo)
            .toList();
    }

    @Override
    public SfFarmingRecordTypeVo get(Long typeId) {
        SfFarmingRecordType row = requireType(typeId);
        return toVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(SfFarmingRecordTypeSaveBo bo) {
        String tenantId = requireTenantId();
        String code = normalizeCode(bo.getTypeCode());
        if (typeMapper.existsTenantCode(null, tenantId, code)) {
            throw new ServiceException("类型编码已存在");
        }
        Date now = new Date();
        Long userId = LoginHelper.getUserId();
        SfFarmingRecordType row = new SfFarmingRecordType();
        row.setTypeId(IdWorker.getId());
        row.setTenantId(tenantId);
        row.setTypeCode(code);
        row.setTypeName(normalizeName(bo.getTypeName()));
        row.setListIconUrl(bo.getListIconUrl());
        row.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        row.setStatus(StringUtils.isBlank(bo.getStatus()) ? SystemConstants.NORMAL : bo.getStatus().trim());
        row.setDelFlag(SystemConstants.NORMAL);
        row.setRemark(bo.getRemark());
        row.setCreateBy(userId);
        row.setCreateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        row.setUpdateBy(userId);
        row.setUpdateTime(com.ym.agriculture.shared.common.AgricultureTimes.toLocalDateTime(now));
        return typeMapper.insert(row) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SfFarmingRecordTypeSaveBo bo) {
        SfFarmingRecordType old = requireType(bo.getTypeId());
        String code = normalizeCode(bo.getTypeCode());
        if (typeMapper.existsTenantCode(Collections.singleton(old.getTypeId()), old.getTenantId(), code)) {
            throw new ServiceException("类型编码已存在");
        }
        old.setTypeCode(code);
        old.setTypeName(normalizeName(bo.getTypeName()));
        old.setListIconUrl(bo.getListIconUrl());
        old.setSortOrder(bo.getSortOrder() == null ? 0 : bo.getSortOrder());
        if (StringUtils.isNotBlank(bo.getStatus())) {
            old.setStatus(bo.getStatus().trim());
        }
        old.setRemark(bo.getRemark());
        old.setUpdateBy(LoginHelper.getUserId());
        old.setUpdateTime(java.time.LocalDateTime.now());
        return typeMapper.updateById(old) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long typeId) {
        requireType(typeId);
        return typeMapper.deleteById(typeId) > 0;
    }

    private SfFarmingRecordType requireType(Long typeId) {
        String tenantId = requireTenantId();
        SfFarmingRecordType row = typeMapper.selectById(typeId);
        if (row == null || !SystemConstants.NORMAL.equals(row.getDelFlag())) {
            throw new ServiceException("类型不存在");
        }
        if (!tenantId.equals(row.getTenantId())) {
            throw new ServiceException("无权访问该类型");
        }
        return row;
    }

    private SfFarmingRecordTypeVo toVo(SfFarmingRecordType row) {
        return MapstructUtils.convert(row, SfFarmingRecordTypeVo.class);
    }

    private static String normalizeCode(String typeCode) {
        if (StringUtils.isBlank(typeCode)) {
            throw new ServiceException("类型编码不能为空");
        }
        return typeCode.trim();
    }

    private static String normalizeName(String typeName) {
        if (StringUtils.isBlank(typeName)) {
            throw new ServiceException("类型名称不能为空");
        }
        return typeName.trim();
    }

    private static String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("租户上下文缺失");
        }
        return tenantId;
    }
}
