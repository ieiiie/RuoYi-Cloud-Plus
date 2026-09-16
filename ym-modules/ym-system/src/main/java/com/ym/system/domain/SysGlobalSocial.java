package com.ym.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import com.ym.common.mybatis.core.domain.BaseEntity;

/**
 * 全局第三方账号绑定 sys_global_social。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_global_social")
public class SysGlobalSocial extends BaseEntity {

    @TableId(value = "id")
    private Long id;

    /** 所属全局账号ID。 */
    private Long globalUserId;

    private String authId;
    private String source;
    private String accessToken;
    private Integer expireIn;
    private String refreshToken;
    private String openId;
    private String userName;
    private String nickName;
    private String email;
    private String avatar;
    private String accessCode;
    private String unionId;
    private String scope;
    private String tokenType;
    private String idToken;
    private String macAlgorithm;
    private String macKey;
    private String code;
    private String oauthToken;
    private String oauthTokenSecret;

    @TableLogic
    private String delFlag;
}
