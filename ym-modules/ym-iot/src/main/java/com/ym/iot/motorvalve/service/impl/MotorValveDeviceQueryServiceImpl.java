package com.ym.iot.motorvalve.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.service.IIotDataPointService;
import com.ym.iot.hfzk.service.IHfzkDeviceTypeQueryService;
import com.ym.iot.motorvalve.domain.vo.MotorValveDeviceRowVo;
import com.ym.iot.motorvalve.enums.ValveType;
import com.ym.iot.motorvalve.service.IMotorValveDeviceQueryService;
import com.ym.iot.motorvalve.support.MotorvalveValveTypeResolver;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 电动阀设备只读查询，目录、阀型与实时状态均取自 JetLinks。 */
@Service
@RequiredArgsConstructor
public class MotorValveDeviceQueryServiceImpl implements IMotorValveDeviceQueryService {

    private final com.ym.iot.jetlinks.service.IJetLinksTagService jetLinksTags;
    private final com.ym.iot.jetlinks.service.IJetLinksProductService jetLinksProducts;
    private final com.ym.iot.jetlinks.service.IJetLinksDeviceService jetLinksDevices;

    private final IHfzkDeviceTypeQueryService hfzkDeviceTypeQueryService;
    private final IIotDataPointService dataPointService;

    @Override
    public List<MotorValveDeviceRowVo> list(String onlineStatus) {
        return enrich(hfzkDeviceTypeQueryService.listByDeviceType("MOTORVALVE", onlineStatus));
    }

    @Override
    public PageResult<MotorValveDeviceRowVo> page(
            String onlineStatus, String deviceCode, PageQuery pageQuery) {
        PageResult<IotDeviceVo> page =
                hfzkDeviceTypeQueryService.pageByDeviceType(
                        "MOTORVALVE", onlineStatus, deviceCode, pageQuery);
        return PageResult.build(enrich(new ArrayList<>(page.getRows())), page.getTotal());
    }

    private List<MotorValveDeviceRowVo> enrich(List<IotDeviceVo> devices) {
        if (CollUtil.isEmpty(devices)) {
            return List.of();
        }
        List<Long> deviceIds =
                devices.stream().map(IotDeviceVo::getDeviceId).filter(Objects::nonNull).toList();
        Map<Long, String> channelTags;
        {
            channelTags = new java.util.LinkedHashMap<>();
            for (Long id : deviceIds)
                for (var tag : jetLinksTags.queryByDeviceId(id))
                    if (MotorvalveValveTypeResolver.TAG_KEY_CHANNEL.equals(tag.getTagKey()))
                        channelTags.put(id, tag.getTagValue());
        }
        if (channelTags == null) {
            channelTags = Map.of();
        }
        Map<Long, String> productKeys = loadProductKeys(devices);
        Map<Long, IotLatestVo> latestMap = dataPointService.getLatestMap(deviceIds);
        if (latestMap == null) {
            latestMap = Map.of();
        }

        List<MotorValveDeviceRowVo> result = new ArrayList<>(devices.size());
        for (IotDeviceVo device : devices) {
            MotorValveDeviceRowVo row =
                    BeanUtil.copyProperties(device, MotorValveDeviceRowVo.class);
            IotLatestVo latest =
                    device.getDeviceId() == null ? null : latestMap.get(device.getDeviceId());
            row.setLatest(latest);
            if (latest != null && latest.getCollectTimes() != null) {
                latest.getCollectTimes().values().stream()
                        .filter(Objects::nonNull)
                        .max(java.util.Date::compareTo)
                        .ifPresent(row::setLastReportTime);
            }
            String productKey =
                    StrUtil.blankToDefault(
                            device.getProductKey(),
                            device.getProductId() == null
                                    ? null
                                    : productKeys.get(device.getProductId()));
            ValveType type =
                    com.ym.iot.jetlinks.support.JetLinksValveMetadata.configured(
                            jetLinksDevices.get(device.getDeviceId()).data());
            row.setProductKey(productKey);
            row.setValveType(type.getCode());
            row.setValveTypeLabel(
                    MotorvalveValveTypeResolver.resolveDisplayLabel(type, productKey));
            result.add(row);
        }
        return result;
    }

    private Map<Long, String> loadProductKeys(List<IotDeviceVo> devices) {
        List<Long> productIds =
                devices.stream()
                        .map(IotDeviceVo::getProductId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        for (Long id : productIds) {
            var product = jetLinksProducts.queryById(id);
            if (product != null) result.put(id, product.getProductKey());
        }
        return result;
    }
}
