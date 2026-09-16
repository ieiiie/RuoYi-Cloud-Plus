package com.ym.iot.motorvalve.domain.vo;

import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.device.domain.vo.IotLatestVo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 电动阀设备行视图 — 在 {@link IotDeviceVo} 基础上附加已解析的阀型信息。
 *
 * <p>阀型通过 {@link com.ym.iot.motorvalve.support.MotorvalveValveTypeResolver} 优先级链解析：
 * product_key > configJson > iot_device_tag(chanel) > 设备名关键词 > 默认单通。
 * 前端可直接使用 {@code valveType} 决定显示格式（单通→百分比%, 三通/五通→角度°+出口方向），
 * 不再需要本地推断。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MotorValveDeviceRowVo extends IotDeviceVo {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 已解析的阀门类型编码，对应 {@link com.ym.iot.motorvalve.enums.ValveType} 枚举的 code 字段。
     * 取值：single_port / three_way_l / three_way_t / five_port。
     * 前端可据此决定位置显示格式与控制界面布局。
     */
    private String valveType;

    /**
     * 阀门类型中文标签（如"单通阀"、"分体阀"、"三通L型"、"五通阀"），方便前端直接展示。
     * 分体阀的控制算法为 single_port，但展示名称保留产品形态。
     */
    private String valveTypeLabel;

    /**
     * 设备最新遥测数据；电动阀列表接口固定绕过 Redis latest 缓存，直接读取 InfluxDB 最新值。
     */
    private IotLatestVo latest;
}
