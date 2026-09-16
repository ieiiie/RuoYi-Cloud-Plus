package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.tenant.core.TenantEntity;

import java.time.LocalDateTime;

/**
 * 租户成员对象 sys_user。
 *
 * <p>用户名、手机号和密码仅保存在 {@link SysGlobalUser}；本表只保存某个全局账号
 * 在当前租户的成员关系、组织与授权属性。</p>
 *
 * @author Lion Li
 */

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends TenantEntity {

    /**
     * 用户ID
     */
    @TableId(value = "user_id")
    private Long userId;

    /**
     * 全局账号ID。
     *
     * <p>同一全局账号可在多个租户各拥有一条本地用户记录。</p>
     */
    private Long globalUserId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 用户昵称
     */
    private String nickName;

    /**
     * 用户类型（sys_user系统用户）
     */
    private String userType;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户性别
     */
    private String gender;

    /**
     * 用户头像
     */
    private Long avatar;

    /**
     * 账号状态（0正常 1停用）
     */
    private String status;

    /**
     * 删除标志（0代表存在 1代表删除）
     */
    @TableLogic
    private String delFlag;

    /**
     * 最后登录IP
     */
    private String loginIp;

    /**
     * 最后登录时间
     */
    private LocalDateTime loginDate;

    /**
     * 备注
     */
    private String remark;


    /**
     * 使用用户ID构造系统用户对象。
     *
     * @param userId 用户ID
     */
    public SysUser(Long userId) {
        this.userId = userId;
    }

    /**
     * 判断当前用户是否为超级管理员。
     *
     * @return true 是超级管理员 false 不是超级管理员
     */
    public boolean isSuperAdmin() {
        return SystemConstants.SUPER_ADMIN_USER_ID.equals(this.userId);
    }

}
