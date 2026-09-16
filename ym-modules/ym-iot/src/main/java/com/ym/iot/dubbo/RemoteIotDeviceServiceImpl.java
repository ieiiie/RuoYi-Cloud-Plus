package com.ym.iot.dubbo;

import cn.hutool.core.bean.BeanUtil;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.iot.api.domain.vo.RemoteProductVo;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.device.service.IIotDeviceService;

import lombok.RequiredArgsConstructor;

import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 物联网设备跨服务查询实现。
 *
 * @author ym-cloud
 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteIotDeviceServiceImpl implements RemoteIotDeviceService {

    private final com.ym.iot.jetlinks.service.IJetLinksDeviceService jetLinksDevices;
    private final com.ym.iot.jetlinks.service.IJetLinksProductService jetLinksProducts;
    private final IIotDeviceService deviceService;

    @Override
    public RemoteDeviceSummaryVo getDevice(Long deviceId) {
        return toSummary(deviceService.queryById(deviceId));
    }

    @Override
    public List<RemoteDeviceSummaryVo> listDevices(Collection<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return List.of();
        }
        return jetLinksDevices.byIds(deviceIds).stream()
                .map(RemoteIotDeviceServiceImpl::toSummary)
                .toList();
    }

    @Override
    public List<RemoteDeviceSummaryVo> listDevicesByCodes(Collection<String> deviceCodes) {
        IotDeviceBo bo = new IotDeviceBo();
        bo.setDeviceCodeList(
                deviceCodes == null
                        ? List.of()
                        : deviceCodes.stream()
                                .filter(code -> code != null && !code.isBlank())
                                .map(String::trim)
                                .distinct()
                                .toList());
        if (bo.getDeviceCodeList().isEmpty()) {
            return List.of();
        }
        return deviceService.queryList(bo).stream()
                .map(RemoteIotDeviceServiceImpl::toSummary)
                .toList();
    }

    @Override
    public List<RemoteDeviceSummaryVo> listDevices(RemoteDeviceQueryBo query) {
        RemoteDeviceQueryBo safe = query == null ? new RemoteDeviceQueryBo() : query;
        IotDeviceBo bo = toDeviceBo(safe);
        return deviceService.queryList(bo).stream()
                .map(RemoteIotDeviceServiceImpl::toSummary)
                .toList();
    }

    @Override
    public List<RemoteProductVo> listProductsByIds(Collection<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return productIds.stream()
                .map(jetLinksProducts::queryById)
                .filter(java.util.Objects::nonNull)
                .map(row -> BeanUtil.toBean(row, RemoteProductVo.class))
                .toList();
    }

    @Override
    public List<RemoteProductVo> listProductsByKeys(Collection<String> productKeys) {
        if (productKeys == null || productKeys.isEmpty()) {
            return List.of();
        }
        return jetLinksProducts.queryList(null).stream()
                .filter(row -> productKeys.contains(row.getProductKey()))
                .map(row -> BeanUtil.toBean(row, RemoteProductVo.class))
                .toList();
    }

    @Override
    public PageResult<RemoteDeviceSummaryVo> pageDevices(RemoteDeviceQueryBo query) {
        RemoteDeviceQueryBo safe = query == null ? new RemoteDeviceQueryBo() : query;
        IotDeviceBo bo = toDeviceBo(safe);
        PageQuery pageQuery = new PageQuery(safe.getPageSize(), safe.getPageNum());
        PageResult<IotDeviceVo> page = deviceService.queryPageList(bo, pageQuery);
        List<RemoteDeviceSummaryVo> rows =
                page.getRows().stream().map(RemoteIotDeviceServiceImpl::toSummary).toList();
        return PageResult.build(rows, page.getTotal());
    }

    private static RemoteDeviceSummaryVo toSummary(IotDeviceVo device) {
        if (device == null) {
            return null;
        }
        return BeanUtil.toBean(device, RemoteDeviceSummaryVo.class);
    }

    private static IotDeviceBo toDeviceBo(RemoteDeviceQueryBo query) {
        IotDeviceBo bo = new IotDeviceBo();
        bo.setProductId(query.getProductId());
        bo.setDeviceCode(query.getDeviceCode());
        bo.setDeviceCodeList(query.getDeviceCodeList());
        bo.setDeviceName(query.getDeviceName());
        bo.setDeviceCategory(query.getDeviceCategory());
        bo.setOnlineStatus(query.getOnlineStatus());
        bo.setStatus(query.getStatus());
        return bo;
    }
}
