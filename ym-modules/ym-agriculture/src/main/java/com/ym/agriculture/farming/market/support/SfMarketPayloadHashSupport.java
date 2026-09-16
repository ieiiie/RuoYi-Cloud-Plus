package com.ym.agriculture.farming.market.support;

import cn.hutool.core.util.StrUtil;
import org.erdtman.jcs.JsonCanonicalizer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/** 农业行情原始载荷 RFC 8785（JCS）摘要计算与校验。 */
@Component
public class SfMarketPayloadHashSupport {

    private static final Pattern LOWERCASE_SHA256 = Pattern.compile("[0-9a-f]{64}");

    /** 校验爬虫提交的摘要格式及其与原始载荷的一致性。 */
    public void validate(String rawPayloadJson, String submittedHash) {
        if (StrUtil.isBlank(submittedHash) || !LOWERCASE_SHA256.matcher(submittedHash).matches()) {
            throw new PayloadHashException("PAYLOAD_HASH_FORMAT_ERROR", "payload_sha256 必须为64位小写十六进制");
        }
        String calculatedHash = calculate(rawPayloadJson);
        boolean matched = MessageDigest.isEqual(
            calculatedHash.getBytes(StandardCharsets.US_ASCII),
            submittedHash.getBytes(StandardCharsets.US_ASCII));
        if (!matched) {
            throw new PayloadHashException("PAYLOAD_HASH_MISMATCH", "payload_sha256 与 raw_payload_json 不一致");
        }
    }

    /** 按 RFC 8785 JCS + UTF-8 + SHA-256 计算64位小写十六进制摘要。 */
    public String calculate(String rawPayloadJson) {
        if (StrUtil.isBlank(rawPayloadJson)) {
            throw new PayloadHashException("PAYLOAD_CANONICALIZATION_ERROR", "raw_payload_json 不能为空");
        }
        try {
            byte[] canonicalPayload = new JsonCanonicalizer(rawPayloadJson).getEncodedUTF8();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonicalPayload));
        } catch (PayloadHashException e) {
            throw e;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("当前JVM不支持SHA-256", e);
        } catch (IOException | RuntimeException e) {
            throw new PayloadHashException("PAYLOAD_CANONICALIZATION_ERROR", "raw_payload_json 不是有效的JCS载荷", e);
        }
    }

    /** 可回写暂存处理错误码的载荷摘要校验异常。 */
    public static class PayloadHashException extends IllegalArgumentException {
        private final String errorCode;

        public PayloadHashException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public PayloadHashException(String errorCode, String message, Throwable cause) {
            super(message, cause);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}
