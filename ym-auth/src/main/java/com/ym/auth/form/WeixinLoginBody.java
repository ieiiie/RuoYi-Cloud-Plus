package com.ym.auth.form;

import lombok.Data;

/** 微信小程序兼容登录参数。 */
@Data
public class WeixinLoginBody {
    private String clientId;
    private String grantType;
    private String appid;
    /** 历史客户端字段。 */
    private String code;
    /** 新认证策略字段。 */
    private String xcxCode;
}
