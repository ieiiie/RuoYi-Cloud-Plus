package com.ym.agriculture.farming.integration.ai.algback.client;

import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackResultVo;
import lombok.Getter;

import java.io.IOException;

/**
 * 调用算法中台失败：HTTP 非 2xx、IO，或中台 {@code code != 200}。
 * <p>
 * {@link #getHttpCode()}：HTTP 码；业务失败且已解析 envelope 时可能为 200；纯文案构造时为 {@code null}。<br>
 * {@link #getBusinessCode()}：中台 JSON {@code code}，由 {@link AlgBackResultVo} 构造时可能非空。
 *
 * @author ym-cloud
 */
@Getter
public class AlgBackClientException extends RuntimeException {

    /** HTTP 响应码；非 HTTP 错误场景可能为 {@code null}。 */
    private final Integer httpCode;

    /** 中台 {@link AlgBackResultVo#getCode()}，业务失败时使用。 */
    private final Integer businessCode;

    /** 通用文案；{@link #httpCode}、{@link #businessCode} 均为空。 */
    public AlgBackClientException(String message) {
        super(message);
        this.httpCode = null;
        this.businessCode = null;
    }

    /** IO 等异常包装。 */
    public AlgBackClientException(String message, Throwable cause) {
        super(message, cause);
        this.httpCode = null;
        this.businessCode = null;
    }

    /** HTTP 非 2xx；{@code bodySnippet} 为 errorBody 文本（可能为空）。 */
    public AlgBackClientException(int httpCode, String bodySnippet) {
        super("算法中台 HTTP " + httpCode + (bodySnippet == null || bodySnippet.isEmpty() ? "" : ": " + bodySnippet));
        this.httpCode = httpCode;
        this.businessCode = null;
    }

    /** HTTP 200 但业务 {@code code != 200}。 */
    public AlgBackClientException(AlgBackResultVo<?> envelope) {
        super("算法中台业务错误: code=" + envelope.getCode() + ", msg=" + envelope.getMsg());
        this.httpCode = 200;
        this.businessCode = envelope.getCode();
    }

    /** {@link AlgBackSync} 捕获 {@link IOException} 时转换。 */
    public static AlgBackClientException fromIo(IOException e) {
        return new AlgBackClientException("调用算法中台失败: " + e.getMessage(), e);
    }
}
