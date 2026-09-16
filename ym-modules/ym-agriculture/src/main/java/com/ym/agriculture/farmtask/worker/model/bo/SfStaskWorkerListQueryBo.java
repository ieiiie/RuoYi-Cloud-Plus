package com.ym.agriculture.farmtask.worker.model.bo;

import lombok.Data;

/**
 * stask 小程序人员列表查询条件。
 */
@Data
public class SfStaskWorkerListQueryBo {

    /**
     * 搜索关键字，匹配姓名或手机号。
     */
    private String keyword;

    /**
     * 应用角色编码：stask:production_admin、stask:expert、stask:group_leader、stask:worker。
     */
    private String appRoleCode;

    /**
     * 人员状态：0-在职/正常，1-离职/停用。
     */
    private String status;
}
