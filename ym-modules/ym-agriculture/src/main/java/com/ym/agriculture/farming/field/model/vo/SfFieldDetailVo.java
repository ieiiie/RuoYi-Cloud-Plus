package com.ym.agriculture.farming.field.model.vo;

import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.iot.api.domain.vo.RemoteDeviceSummaryVo;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.List;

/**
 * 地块详情视图：在 {@link SfFieldVo} 基础上增加统计字段。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfFieldDetailVo extends SfFieldVo {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 当前地块下未删除的种植批次数量 */
    private Long batchCount;

    /** 已绑定的物联网设备列表（详情接口填充） */
    private List<RemoteDeviceSummaryVo> iotDevices;

    /** 当前地块下的种植批次列表（按权限过滤） */
    private List<SfPlantingBatchVo> plantingBatches;

    /** 当前进行中的种植批次信息；无进行中批次时为 null */
    private SfFieldBatchInfoVo activeBatch;

    /**
     * 已绑定的无人机设备名称（若存在）。
     * <p>
     * 该名称来自飞控平台设备列表，与 {@link #uavDeviceSn} 对应，不是物联网设备名称。
     */
    private String uavDeviceName;

    /**
     * 已绑定的无人机在线状态（来自飞控设备列表启发式解析）；飞控不可用时为 null。
     */
    private Boolean uavDeviceOnline;

    /** 归属用户名称（跨库 sys_user 查询填充） */
    private String ownerUserName;
}
