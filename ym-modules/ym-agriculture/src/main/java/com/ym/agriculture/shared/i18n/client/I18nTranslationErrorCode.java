package com.ym.agriculture.shared.i18n.client;

/**
 * 翻译提供商调用的稳定错误码。
 */
public final class I18nTranslationErrorCode {

    /** 功能未启用。 */
    public static final String CONFIG_DISABLED = "CONFIG_DISABLED";

    /** 配置缺失或非法。 */
    public static final String CONFIG_INVALID = "CONFIG_INVALID";

    /** 提供商鉴权失败。 */
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";

    /** 提供商拒绝了请求参数。 */
    public static final String INVALID_REQUEST = "INVALID_REQUEST";

    /** 提供商限流。 */
    public static final String RATE_LIMITED = "RATE_LIMITED";

    /** 提供商服务端暂时异常。 */
    public static final String PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";

    /** 网络连接异常。 */
    public static final String NETWORK_ERROR = "NETWORK_ERROR";

    /** 调用超时。 */
    public static final String TIMEOUT = "TIMEOUT";

    /** 提供商响应格式非法或无有效译文。 */
    public static final String INVALID_RESPONSE = "INVALID_RESPONSE";

    /** 未分类的调用异常。 */
    public static final String UNKNOWN = "UNKNOWN";

    /** 摘要相同但原文不同，拒绝使用可疑译文。 */
    public static final String SOURCE_HASH_COLLISION = "SOURCE_HASH_COLLISION";

    private I18nTranslationErrorCode() {
    }
}
