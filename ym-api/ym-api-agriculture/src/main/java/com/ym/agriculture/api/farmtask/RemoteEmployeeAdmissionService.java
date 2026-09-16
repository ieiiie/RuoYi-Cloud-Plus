package com.ym.agriculture.api.farmtask;

import com.ym.agriculture.api.farmtask.domain.vo.RemoteEmployeeAdmissionVo;

/** 员工小程序业务准入契约。 */
public interface RemoteEmployeeAdmissionService {

    /** 按当前显式租户校验微信人员是否可进入业务。 */
    RemoteEmployeeAdmissionVo checkByOpenid(String openid);
}
