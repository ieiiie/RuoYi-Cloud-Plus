package org.dromara.system.domain.bo;

import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.dromara.common.core.validate.EditGroup;
import org.dromara.common.core.xss.Xss;
import org.dromara.system.domain.SysGlobalUser;

import java.io.Serial;
import java.io.Serializable;

/**
 * 全局账号管理业务对象。
 */
@Data
@AutoMapper(target = SysGlobalUser.class, reverseConvertGenerate = false)
public class SysGlobalUserBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "全局账号ID不能为空", groups = EditGroup.class)
    private Long globalUserId;

    @Xss(message = "用户账号不能包含脚本字符")
    @Size(min = 2, max = 30, message = "用户账号长度必须在{min}到{max}个字符之间")
    private String userName;

    @Xss(message = "用户昵称不能包含脚本字符")
    @Size(max = 30, message = "用户昵称长度不能超过{max}个字符")
    private String nickName;

    private String userType;

    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过{max}个字符")
    private String email;

    private String phoneNumber;
    private String gender;
    private Long avatar;
    private String status;
    private String remark;
}
