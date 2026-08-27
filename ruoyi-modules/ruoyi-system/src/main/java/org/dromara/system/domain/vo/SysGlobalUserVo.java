package org.dromara.system.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import org.dromara.system.domain.SysGlobalUser;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局账号视图对象。
 */
@Data
@AutoMapper(target = SysGlobalUser.class)
public class SysGlobalUserVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long globalUserId;
    private String userName;
    private String nickName;
    private String userType;
    private String email;
    private String phoneNumber;
    private String gender;
    private Long avatar;
    private String status;
    private String remark;
}
