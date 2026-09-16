package com.ym.agriculture.farming.integration.ai.algback.dto.login;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * 登录成功 {@code data}；业务侧主要使用 {@link #token}。
 *
 * @author ym-cloud
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackLoginData {

    /** 后续业务请求 Header {@code token}；须仍在 Redis/会话有效期内。 */
    private String token;

    /** 用户信息；管理端字段，可选解析。 */
    private JsonNode userInfo;

    /** 路由/权限；对接方可忽略。 */
    private JsonNode routerVoList;
}
