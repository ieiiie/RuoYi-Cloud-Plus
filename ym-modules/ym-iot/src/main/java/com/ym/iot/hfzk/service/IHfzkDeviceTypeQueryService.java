package com.ym.iot.hfzk.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.vo.IotDeviceVo;

import java.util.List;

/**
 * 按 device_type 从 HFZK 登记表关联 iot_device 查询设备。
 *
 * <p>查询链路：{@code iot_hfzk_user_device (device_type=?) → external_device_id → iot_device (device_code)}
 *
 * @author ym-cloud
 */
public interface IHfzkDeviceTypeQueryService {

    /**
     * 按设备类型查 iot_device 列表（不分页）。
     *
     * @param deviceType HFZK 设备类型（如 MOTORVALVE、FERTILIZER、WVP_CAMERA）
     * @param onlineStatus 在线状态筛选（可选：ONLINE/OFFLINE/FAULT）
     * @return 设备列表
     */
    List<IotDeviceVo> listByDeviceType(String deviceType, String onlineStatus);

    /**
     * 按设备类型分页查 iot_device。
     *
     * @param deviceType HFZK 设备类型
     * @param onlineStatus 在线状态筛选（可选）
     * @param deviceCode 设备编号模糊筛选（可选）
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    PageResult<IotDeviceVo> pageByDeviceType(String deviceType, String onlineStatus,
                                              String deviceCode, PageQuery pageQuery);
}
