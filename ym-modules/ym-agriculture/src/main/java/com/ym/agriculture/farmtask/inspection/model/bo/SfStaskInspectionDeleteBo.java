package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

/** 删除抽检参数。 */
@Data
public class SfStaskInspectionDeleteBo {

    /** 客户端读取到的乐观锁版本。 */
    private Long version;
}
