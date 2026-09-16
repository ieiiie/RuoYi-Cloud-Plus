package com.ym.agriculture.api.farmtask.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 生产管理员或技术员验收命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteTaskAcceptanceBo extends RemoteTaskCommandBo {
    @Serial
    private static final long serialVersionUID = 1L;
    private String result;
    private String rejectReason;
    private String acceptancePhotos;
    private String acceptorRoleCode;
}
