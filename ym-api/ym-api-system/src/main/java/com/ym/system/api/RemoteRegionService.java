package com.ym.system.api;

/** 全局行政区划查询契约。 */
public interface RemoteRegionService {
    /** 按六位 adcode 查询行政区划名称。 */
    String getRegionName(String adcode);
}
