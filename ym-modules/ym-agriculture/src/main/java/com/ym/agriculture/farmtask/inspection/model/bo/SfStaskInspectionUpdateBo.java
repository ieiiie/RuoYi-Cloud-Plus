package com.ym.agriculture.farmtask.inspection.model.bo;

import lombok.Data;

import java.time.LocalDate;

/** 修改抽检参数。 */
@Data
public class SfStaskInspectionUpdateBo {

    /** 问题发现日期。 */
    private LocalDate foundDate;
    /** 大棚 ID。 */
    private Long greenhouseId;
    /** 关联拆分工单 ID，可空。 */
    private Long orderId;
    /** 严重程度：NORMAL-一般，SERIOUS-严重。 */
    private String severity;
    /** 问题描述，最多500字。 */
    private String problemDescription;
    /** 问题照片 JSON 字符串数组，最多6项。 */
    private String problemPhotoJson;
    /** 负责技术员员工 ID。 */
    private Long responsibleTechnicianEmployeeId;
    /** 负责组长员工 ID，可空。 */
    private Long responsibleLeaderEmployeeId;
    /** 客户端读取到的乐观锁版本。 */
    private Long version;
}
