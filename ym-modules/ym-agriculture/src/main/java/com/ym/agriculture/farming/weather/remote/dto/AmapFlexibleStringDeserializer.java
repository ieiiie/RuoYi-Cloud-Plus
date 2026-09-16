package com.ym.agriculture.farming.weather.remote.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 高德部分字段在 JSON 中可能为字符串或数字，统一反序列化为字符串。
 */
public final class AmapFlexibleStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) {
            return null;
        }
        if (t == JsonToken.VALUE_NUMBER_INT || t == JsonToken.VALUE_NUMBER_FLOAT) {
            return p.getNumberValue().toString();
        }
        return p.getValueAsString();
    }
}
