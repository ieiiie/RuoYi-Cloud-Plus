package com.ym.iot.device.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceExportVo;
import com.ym.iot.device.domain.vo.IotDeviceVo;

import java.util.List;

/**
 * 物联网设备业务接口。
 * <p>
 * 设备归属于产品，deviceCode 租户内唯一。status：0 正常，1 停用；onlineStatus：ONLINE/OFFLINE/FAULT。
 * </p>
 *
 * @author ym-cloud
 */
public interface IIotDeviceService {

    /** 按条件查询设备列表。 */
    List<IotDeviceVo> queryList(IotDeviceBo bo);

    /** 分页查询设备列表。 */
    PageResult<IotDeviceVo> queryPageList(IotDeviceBo bo, PageQuery pageQuery);

    /** 根据主键查询设备详情。 */
    IotDeviceVo queryById(Long deviceId);

    /** 查询导出用列表。 */
    List<IotDeviceExportVo> queryExportList(IotDeviceBo bo);
}
