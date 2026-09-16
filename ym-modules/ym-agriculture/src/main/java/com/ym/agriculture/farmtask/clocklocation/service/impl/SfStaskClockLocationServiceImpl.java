package com.ym.agriculture.farmtask.clocklocation.service.impl;

import com.ym.agriculture.farmtask.clocklocation.dao.SfStaskClockLocationMapper;
import com.ym.agriculture.farmtask.clocklocation.model.bo.SfStaskClockLocationBo;
import com.ym.agriculture.farmtask.clocklocation.model.entity.SfStaskClockLocation;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.service.ISfStaskClockLocationService;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.tenant.helper.TenantHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 打卡地点服务实现。
 */
@Service
@RequiredArgsConstructor
public class SfStaskClockLocationServiceImpl implements ISfStaskClockLocationService {

    private static final String COORDINATE_TYPE = "CGCS2000";
    private static final String MAP_PROVIDER = "tianditu";

    private final SfStaskClockLocationMapper clockLocationMapper;

    @Override
    public SfStaskClockLocationVo getCurrent() {
        return getByTenantId(requireTenantId());
    }

    @Override
    public SfStaskClockLocationVo getByTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new ServiceException("租户编号不能为空");
        }
        SfStaskClockLocation entity = TenantHelper.dynamic(tenantId,
            () -> clockLocationMapper.selectByTenantId(tenantId));
        return entity == null ? null : toVo(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SfStaskClockLocationBo bo) {
        String tenantId = requireTenantId();
        SfStaskClockLocation entity = clockLocationMapper.selectByTenantId(tenantId);
        if (entity == null) {
            entity = new SfStaskClockLocation();
            entity.setTenantId(tenantId);
        }
        entity.setLocationName(bo.getLocationName().trim());
        entity.setCenterLng(bo.getCenterLng());
        entity.setCenterLat(bo.getCenterLat());
        entity.setCoordinateType(COORDINATE_TYPE);
        entity.setRadiusMeters(bo.getRadiusMeters());
        entity.setEnabled(bo.getEnabled());
        entity.setMapProvider(MAP_PROVIDER);
        entity.setMapZoomLevel(bo.getMapZoomLevel());
        entity.setAddressText(bo.getAddressText());
        entity.setRemark(bo.getRemark());
        if (entity.getClockLocationId() == null) {
            clockLocationMapper.insert(entity);
        } else {
            clockLocationMapper.updateById(entity);
        }
    }

    private SfStaskClockLocationVo toVo(SfStaskClockLocation entity) {
        SfStaskClockLocationVo vo = new SfStaskClockLocationVo();
        vo.setClockLocationId(entity.getClockLocationId());
        vo.setLocationName(entity.getLocationName());
        vo.setCenterLng(entity.getCenterLng());
        vo.setCenterLat(entity.getCenterLat());
        vo.setCoordinateType(entity.getCoordinateType());
        vo.setRadiusMeters(entity.getRadiusMeters());
        vo.setEnabled(entity.getEnabled());
        vo.setMapProvider(entity.getMapProvider());
        vo.setMapZoomLevel(entity.getMapZoomLevel());
        vo.setAddressText(entity.getAddressText());
        vo.setRemark(entity.getRemark());
        vo.setUpdateTime(entity.getUpdateTime());
        vo.setConfigured(true);
        vo.setFenceCheckRequired("1".equals(entity.getEnabled())
            && entity.getCenterLng() != null
            && entity.getCenterLat() != null
            && entity.getRadiusMeters() != null
            && entity.getRadiusMeters() > 0);
        return vo;
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            throw new ServiceException("缺少租户上下文");
        }
        return tenantId;
    }
}
