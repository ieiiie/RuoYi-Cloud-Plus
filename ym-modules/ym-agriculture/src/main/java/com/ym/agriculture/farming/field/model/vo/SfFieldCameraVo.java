package com.ym.agriculture.farming.field.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 地块绑定的 WVP 摄像头视图对象。
 * <p>
 * {@link #deviceSn} 即 WVP 国标编号，与 {@code iot_device.device_code} 对齐；
 * {@link #channelIds} 来自 {@code iot_hfzk_user_device.extra_info} 缓存的 WVP 通道快照
 * （由 WVP 设备同步任务回填，元素结构详见 SQL 注释；取每项 {@code id} 字段）。
 *
 * @author ym-cloud
 */
@Data
public class SfFieldCameraVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 设备 SN（WVP 国标编号） */
    private String deviceSn;

    /** 设备名称（来自 {@code iot_hfzk_user_device.device_name}） */
    private String deviceName;

    /**
     * 在线状态来源 {@code iot_device.online_status}：{@code ONLINE}/{@code OFFLINE}/{@code FAULT}；
     * 平台无对应 {@code device_code} 的摄像头返回 {@code UNKNOWN}。
     */
    private String onlineStatus;

    /** 在线布尔值，便于前端判断；状态未知时为 {@code null} */
    private Boolean online;

    /**
     * WVP 通道 ID 列表（来自 {@code iot_hfzk_user_device.extra_info} 中每项 {@code id} 字段）；
     * 旧记录尚未被同步任务回填、JSON 解析失败或该设备无通道时返回空数组。
     */
    private List<String> channelIds;
}
