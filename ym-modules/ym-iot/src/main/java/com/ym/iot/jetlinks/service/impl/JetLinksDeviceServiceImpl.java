package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;
import static com.ym.iot.jetlinks.support.JetLinksMapping.*;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceExportVo;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksDeviceService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksDeviceServiceImpl implements IJetLinksDeviceService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;

    public QueryDto query(IotDeviceBo bo, PageQuery page) {
        List<String> scope = access.ids();
        if (bo != null && bo.getDeviceId() != null) {
            access.require(bo.getDeviceId());
            scope = List.of(bo.getDeviceId().toString());
        }
        return JetLinksMapping.query(
                scope,
                filters(
                        bo,
                        "productId",
                        "deviceCode",
                        "deviceCodeList",
                        "deviceName",
                        "deviceCategory",
                        "onlineStatus",
                        "status"),
                page);
    }

    public List<RecordDto> records(IotDeviceBo bo) {
        QueryDto q = query(bo, null);
        return all(rpc.getDevice()::devices, q);
    }

    @Override
    public List<IotDeviceVo> queryList(IotDeviceBo bo) {
        return records(bo).stream().map(this::view).toList();
    }

    @Override
    public PageResult<IotDeviceVo> queryPageList(IotDeviceBo bo, PageQuery page) {
        if (page == null || page.getPageSize() == null || page.getPageSize() > 500)
            return slice(queryList(bo), page);
        QueryDto q = query(bo, page);
        var result = await(rpc.getDevice().devices(q));
        return PageResult.build(result.records().stream().map(this::view).toList(), result.total());
    }

    public List<IotDeviceVo> byIds(Collection<Long> ids) {
        access.require(ids);
        return all(
                        rpc.getDevice()::devices,
                        new QueryDto(JetLinksMapping.ids(ids), Map.of(), 1, 500, null, null))
                .stream()
                .map(this::view)
                .toList();
    }

    public RecordDto get(Long id) {
        access.require(id);
        return await(rpc.getDevice().device(id.toString()));
    }

    @Override
    public IotDeviceVo queryById(Long id) {
        return view(get(id));
    }

    private IotDeviceVo view(RecordDto row) {
        IotDeviceVo vo = bean(row, "deviceId", IotDeviceVo.class);
        if (vo == null) return null;
        String secret = vo.getDeviceSecret();
        if (secret != null)
            vo.setDeviceSecretMask(
                    secret.length() > 4 ? "****" + secret.substring(secret.length() - 4) : "****");
        vo.setDeviceSecret(null);
        vo.setTenantId(access.snapshot(vo.getDeviceId()).getTenantId());
        return vo;
    }

    @Override
    public List<IotDeviceExportVo> queryExportList(IotDeviceBo bo) {
        Map<String, String> names = new LinkedHashMap<>();
        for (var category : await(rpc.getCatalog().categories()))
            names.put(category.code(), category.name());
        List<IotDeviceExportVo> rows = beans(records(bo), "deviceId", IotDeviceExportVo.class);
        for (var row : rows)
            if (names.containsKey(row.getDeviceCategory()))
                row.setDeviceCategory(names.get(row.getDeviceCategory()));
        return rows;
    }

    /** Codes are resolved inside the authorized scope, never a global GB/code lookup. */
    public List<RecordDto> byCodes(Collection<String> codes) {
        if (codes == null || codes.isEmpty()) return List.of();
        IotDeviceBo bo = new IotDeviceBo();
        bo.setDeviceCodeList(
                codes.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .toList());
        if (bo.getDeviceCodeList().isEmpty()) return List.of();
        return records(bo);
    }

    public Long requireCode(String code) {
        List<RecordDto> rows = byCodes(List.of(code));
        if (rows.size() != 1) throw new ServiceException("设备不存在、无权访问或设备编号不唯一");
        Long id = id(rows.getFirst().id());
        access.require(id);
        return id;
    }
}
