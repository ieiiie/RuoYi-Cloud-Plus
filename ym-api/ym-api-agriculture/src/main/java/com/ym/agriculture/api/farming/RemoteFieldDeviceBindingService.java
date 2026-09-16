package com.ym.agriculture.api.farming;

/** 设备回收/归属变更前的内部只读占用检查。失败必须拒绝变更，不能按无绑定处理。 */
public interface RemoteFieldDeviceBindingService {
    /** 按设备 SN 统计全部租户尚未解除的地块绑定，不返回任何租户或地块明细。 */
    long countActiveBindings(String deviceSn);
}
