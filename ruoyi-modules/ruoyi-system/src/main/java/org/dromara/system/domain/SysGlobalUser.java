package org.dromara.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * 全局账号对象 sys_global_user。
 *
 * <p>账号凭据和基础资料只在此表保存一份；租户内 {@link SysUser} 通过
 * {@code global_user_id} 关联该账号，并保存本租户的部门、角色、岗位和状态。</p>
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_global_user")
public class SysGlobalUser extends BaseEntity {

    /** 全局账号ID。 */
    @TableId(value = "global_user_id")
    private Long globalUserId;

    /** 用户账号。 */
    private String userName;

    /** 用户昵称。 */
    private String nickName;

    /** 用户类型。 */
    private String userType;

    /** 用户邮箱。 */
    private String email;

    /** 手机号码。 */
    private String phoneNumber;

    /** 用户性别。 */
    private String gender;

    /** 头像 OSS ID。 */
    private Long avatar;

    /** 登录密码。 */
    private String password;

    /** 全局账号状态（0正常 1停用）。 */
    private String status;

    /** 删除标志。 */
    @TableLogic
    private String delFlag;

    /** 备注。 */
    private String remark;
}
