package com.ym.iot.hfzk.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 中科合肥同步用户设备列表的查询条件（业务对象）。
 * <p>
 * 用于 {@link com.ym.iot.hfzk.controller.IotHfzkUserDeviceController} 的 {@code /list}、{@code /page} 接口，
 * 全部为可选条件；多条件之间为 AND。实际数据范围仍受当前登录租户限制，不会跨租户查询。
 * </p>
 *
 * @author ym-cloud
 */
@Data
public class IotHfzkUserDeviceBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 网关侧外部设备 ID（{@code external_device_id}），精确匹配。
     */
    private String externalDeviceId;

    /**
     * 设备序列号（{@code device_sn}），精确匹配。
     */
    private String deviceSn;

    /**
     * 设备类型（{@code device_type}），精确匹配。
     */
    private String deviceType;

    /**
     * 设备名称（{@code device_name}），模糊匹配（LIKE）；传入值会先 trim，空白则不作为条件。
     */
    private String deviceName;
}
