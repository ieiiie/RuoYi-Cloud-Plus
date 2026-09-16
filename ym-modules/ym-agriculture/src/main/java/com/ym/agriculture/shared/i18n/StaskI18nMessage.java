package com.ym.agriculture.shared.i18n;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 在动态参数完成本地化后，使用 stask 消息模板重新生成展示字段。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StaskI18nMessage {

    /** 消息资源键。 */
    String key();

    /** 当前 VO 中按顺序作为 MessageFormat 参数的属性名。 */
    String[] argumentProperties() default {};
}
