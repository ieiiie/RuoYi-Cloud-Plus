package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

import java.time.LocalDate;

/** 创建抽检参数。 */
@Data
public class SfStaskInspectionCreateBo {

    /** 问题发现日期；为空时服务端使用当天。 */
    private LocalDate foundDate;
    /** 大棚 ID。 */
    private Long greenhouseId;
    /** 关联拆分工单 ID，可空。 */
    private Long orderId;
    /** 严重程度：NORMAL-一般，SERIOUS-严重。 */
    private String severity;
    /** 问题描述，1至500字。 */
    private String problemDescription;
    /** 问题照片 JSON 字符串数组，最多6项。 */
    private String problemPhotoJson;
    /** 手工选择的负责技术员 ID；关联工单时由服务端复核。 */
    private Long responsibleTechnicianEmployeeId;
    /** 手工选择的负责组长 ID，可空；关联工单时由服务端复核。 */
    private Long responsibleLeaderEmployeeId;
}
