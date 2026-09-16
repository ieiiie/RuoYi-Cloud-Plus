package com.ym.agriculture.farming.integration.ai.algback.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.OptionalLong;

/**
 * 从 JWT payload 解析 {@code exp}（秒级 Unix）；解析失败返回空。
 *
 * @author ym-cloud
 */
public final class AlgBackJwtExp {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AlgBackJwtExp() {
    }

    public static OptionalLong parseExpEpochSeconds(String jwt) {
        if (jwt == null || jwt.isEmpty()) {
            return OptionalLong.empty();
        }
        String[] parts = jwt.split("\\.");
        if (parts.length < 2) {
            return OptionalLong.empty();
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(padBase64(parts[1]));
            JsonNode root = MAPPER.readTree(payload);
            JsonNode exp = root.get("exp");
            if (exp == null || !exp.isNumber()) {
                return OptionalLong.empty();
            }
            return OptionalLong.of(exp.asLong());
        } catch (Exception ignored) {
            return OptionalLong.empty();
        }
    }

    private static String padBase64(String s) {
        int m = s.length() % 4;
        if (m == 2) {
            return s + "==";
        }
        if (m == 3) {
            return s + "=";
        }
        return s;
    }
}
