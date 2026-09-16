package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

/** 管理员打回抽检参数。 */
@Data
public class SfStaskInspectionRejectBo {

    /** 打回原因，1至500字。 */
    private String rejectReason;
    /** 客户端读取到的乐观锁版本。 */
    private Long version;
}
