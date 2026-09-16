package com.ym.iot.motorvalve.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.motorvalve.domain.bo.ValveBatchControlItemBo;
import com.ym.iot.motorvalve.domain.bo.ValveControlBo;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.bo.ValvePercentControlBo;
import com.ym.iot.motorvalve.domain.vo.MotorValveDeviceRowVo;
import com.ym.iot.motorvalve.domain.vo.ValveBatchControlResultVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveControlProfileVo;
import com.ym.iot.motorvalve.domain.vo.ValveCurrentPercentVo;
import com.ym.iot.motorvalve.domain.vo.ValvePercentControlResultVo;

import java.util.List;

/**
 * 电动阀指令下发 + 控制日志查询服务接口。
 *
 * @author ym-cloud
 */
public interface IValveCommandService {

    /**
     * 查询电动阀设备列表（不分页），含已解析的阀型信息。
     *
     * @param onlineStatus 在线状态筛选（可选：ONLINE/OFFLINE/FAULT）
     * @return 电动阀设备列表（每条含 valveType + valveTypeLabel）
     */
    List<MotorValveDeviceRowVo> listValveDevices(String onlineStatus);

    /**
     * 分页查询电动阀设备，含已解析的阀型信息。
     *
     * @param onlineStatus 在线状态筛选（可选）
     * @param deviceCode   设备编号模糊筛选（可选）
     * @param pageQuery    分页参数
     * @return 分页结果（每条含 valveType + valveTypeLabel）
     */
    PageResult<MotorValveDeviceRowVo> pageValveDevices(String onlineStatus, String deviceCode, PageQuery pageQuery);

    /**
     * 控制阀门。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param bo       控制参数
     * @return 下发的指令文本
     */
    String controlValve(Long deviceId, ValveControlBo bo);

    /**
     * 按百分比控制阀门。
     *
     * <p>平台将百分比换算为厂商协议角度后，复用普通控阀链路下发。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param bo       百分比控制参数
     * @return 换算角度与下发指令
     */
    ValvePercentControlResultVo percentControl(Long deviceId, ValvePercentControlBo bo);

    /**
     * 批量控制多个设备；单个设备内仍只控制一个阀门。
     *
     * @param items 批量控制项，每项包含 deviceId 与控制参数
     * @return 每台设备的下发结果
     */
    List<ValveBatchControlResultVo> batchControl(List<ValveBatchControlItemBo> items);

    /**
     * 查询设备控阀能力（阀门类型、快捷 action 档位）。
     *
     * @param deviceId 设备主键
     * @return 控阀档案
     */
    ValveControlProfileVo getControlProfile(Long deviceId);

    /**
     * 从遥测角度反向计算当前阀门百分比状态。
     *
     * <p>读取设备最新遥测角度，结合阀型反向换算为百分比。
     *
     * @param deviceId 设备主键
     * @return 各阀门当前百分比状态
     */
    ValveCurrentPercentVo getCurrentPercent(Long deviceId);

    /**
     * 读取设备数据。
     *
     * @param deviceId  设备主键
     * @param loraAddrs LoRa阀门地址列表，4G设备传 null 或空
     * @param dataType  数据包类型 1 或 2；null 时按设备 configJson / 全局配置
     * @return 下发的指令文本
     */
    String readData(Long deviceId, List<String> loraAddrs, Integer dataType);

    /**
     * 读取网关状态。
     *
     * @param deviceId 设备主键
     * @return 下发的指令文本
     */
    String readGatewayStatus(Long deviceId);

    /**
     * NTP 时间同步。
     *
     * @param deviceId 设备主键
     * @return 下发的指令文本
     */
    String syncNtpTime(Long deviceId);

    /**
     * 批量读取设备数据。
     *
     * @param deviceIds 设备主键列表
     * @param loraAddrs LoRa阀门地址列表，4G设备传 null 或空
     * @param dataType  数据包类型 1 或 2；null 时按设备 configJson / 全局配置
     * @return 每台设备下发的指令文本列表
     */
    List<String> batchReadData(List<Long> deviceIds, List<String> loraAddrs, Integer dataType);

    /**
     * 分页查询控制日志。
     *
     * @param bo        查询条件
     * @param pageQuery  分页参数
     * @return 分页结果
     */
    PageResult<ValveControlLogVo> queryControlLogPage(ValveControlLogBo bo, PageQuery pageQuery);
}
