package com.ym.common.sensitive.handler;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import com.ym.common.json.enhance.JsonEnhancementContext;
import com.ym.common.json.enhance.JsonFieldContext;
import com.ym.common.json.enhance.JsonFieldProcessor;
import com.ym.common.sensitive.annotation.Sensitive;
import com.ym.common.sensitive.core.SensitiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;

/**
 * 响应脱敏处理器。
 */
@Slf4j
@Order(100)
public class SensitiveJsonFieldProcessor implements JsonFieldProcessor {

    @Autowired(required = false)
    private SensitiveService sensitiveService;

    @Override
    public boolean supports(JsonFieldContext fieldContext) {
        return fieldContext.getAnnotation(Sensitive.class) != null;
    }

    @Override
    public Object process(JsonFieldContext fieldContext, Object value, JsonEnhancementContext context) {
        Sensitive sensitive = fieldContext.getAnnotation(Sensitive.class);
        if (sensitive == null || !(value instanceof String text)) {
            return value;
        }
        if (ObjectUtil.isNotNull(sensitiveService) && sensitiveService.isSensitive(sensitive.roleKey(), sensitive.perms())) {
            return sensitive.strategy().desensitizer().apply(text);
        }
        return text;
    }

}
