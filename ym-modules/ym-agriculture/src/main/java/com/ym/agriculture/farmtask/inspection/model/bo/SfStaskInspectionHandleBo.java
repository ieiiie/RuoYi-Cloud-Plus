package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

/** 技术员处理抽检参数。 */
@Data
public class SfStaskInspectionHandleBo {

    /** 处理说明，1至500字。 */
    private String handleDescription;
    /** 处理照片 JSON 字符串数组，最多6项。 */
    private String handlePhotoJson;
    /** 客户端读取到的乐观锁版本。 */
    private Long version;
}
