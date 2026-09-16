package com.ym.auth.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.core.domain.model.LoginBody;
import org.hibernate.validator.constraints.Length;

/**
 * 用户注册对象
 *
 * @author Lion Li
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RegisterBody extends LoginBody {

    /**
     * 注册目标租户。登录不需要指定租户，但注册仍需明确加入哪个租户。
     */
    @NotBlank(message = "租户编号不能为空")
    private String tenantId;

    /**
     * 用户名
     */
    @NotBlank(message = "{user.username.not.blank}")
    @Length(min = 2, max = 30, message = "{user.username.length.valid}")
    private String username;

    /**
     * 用户密码
     */
    @NotBlank(message = "{user.password.not.blank}")
    @Length(min = 5, max = 30, message = "{user.password.length.valid}")
//    @Pattern(regexp = RegexConstants.PASSWORD, message = "{user.password.format.valid}")
    private String password;

    /**
     * 用户类型
     */
    private String userType;

}
