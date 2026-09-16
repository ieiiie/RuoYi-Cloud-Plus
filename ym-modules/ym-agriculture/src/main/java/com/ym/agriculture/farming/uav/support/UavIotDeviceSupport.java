package com.ym.agriculture.farming.uav.support;

import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.iot.api.RemoteIotDeviceService;
import com.ym.iot.api.domain.bo.RemoteDeviceQueryBo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import com.ym.agriculture.farming.dashboard.support.DashboardSensorOnline;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 无人机/机场台账查询：统计链路统一从 {@code iot_device} 读取（产品 {@code UAV_DOCK}/{@code DJ-UAV}/{@code UAV}）。
 *
 * @author ym-cloud
 */
@Component
public class UavIotDeviceSupport {

    /** 无人机机场产品标识，与 HFZK 同步配置一致 */
    public static final List<String> PRODUCT_KEYS = List.of("UAV_DOCK", "DJ-UAV", "UAV");

    @DubboReference
    private RemoteIotDeviceService iotDeviceService;

    /**
     * 是否为无人机/机场产品设备。
     */
    public static boolean isUavDockDevice(RemoteDeviceSummaryVo device) {
        if (device == null || StringUtils.isBlank(device.getProductKey())) {
            return false;
        }
        String key = device.getProductKey().trim().toUpperCase(Locale.ROOT);
        for (String productKey : PRODUCT_KEYS) {
            if (productKey.equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 是否为无人机/机场（产品 key 或设备名关键词）。
     */
    public static boolean isLikelyUavDock(RemoteDeviceSummaryVo device) {
        if (isUavDockDevice(device)) {
            return true;
        }
        if (device == null || StringUtils.isBlank(device.getDeviceName())) {
            return false;
        }
        String name = device.getDeviceName();
        return name.contains("机场") || name.contains("无人机");
    }

    /**
     * 在线判定，与仪表盘传感器口径一致。
     */
    public static boolean resolveOnline(RemoteDeviceSummaryVo device, Date now) {
        return DashboardSensorOnline.isOnline(device, now);
    }

    /**
     * 当前租户档案正常的无人机/机场设备列表。
     */
    public List<RemoteDeviceSummaryVo> listTenantUavDevices() {
        RemoteDeviceQueryBo bo = new RemoteDeviceQueryBo();
        bo.setStatus(SystemConstants.NORMAL);
        return iotDeviceService.listDevices(bo).stream()
            .filter(UavIotDeviceSupport::isUavDockDevice)
            .toList();
    }

    /**
     * 按设备编号查档案（跨租户查 SN，与地块绑定口径一致）。
     *
     * @param deviceCode 设备 SN / device_code
     * @return 档案正常的设备，未找到返回 null
     */
    public RemoteDeviceSummaryVo findByDeviceCode(String deviceCode) {
        if (StringUtils.isBlank(deviceCode)) {
            return null;
        }
        RemoteDeviceQueryBo bo = new RemoteDeviceQueryBo();
        bo.setStatus(SystemConstants.NORMAL);
        bo.setDeviceCodeList(List.of(deviceCode.trim()));
        List<RemoteDeviceSummaryVo> rows = iotDeviceService.listDevices(bo);
        return rows.stream()
            .filter(d -> d != null && StringUtils.isNotBlank(d.getDeviceCode()))
            .findFirst()
            .orElse(null);
    }
}
