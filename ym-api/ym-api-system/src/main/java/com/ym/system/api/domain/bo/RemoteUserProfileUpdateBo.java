package com.ym.system.api.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 农业员工档案同步到平台账号的幂等命令。 */
@Data
public class RemoteUserProfileUpdateBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String requestId;
    private String businessId;
    private Long userId;
    private String nickName;
    private String phoneNumber;
    private String gender;
    private String status;
}
