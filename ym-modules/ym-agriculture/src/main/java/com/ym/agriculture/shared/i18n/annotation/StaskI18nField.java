package com.ym.agriculture.shared.i18n.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明 stask 视图字段对应的业务翻译资源。
 *
 * <p>同一字段可按优先级声明多个来源；本地化时使用第一个存在成功译文的来源。</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(StaskI18nField.List.class)
public @interface StaskI18nField {

    /**
     * 翻译资源类型。
     */
    String resourceType();

    /**
     * 当前视图对象中承载资源主键的属性名。
     */
    String idProperty();

    /**
     * 翻译表字段键。
     */
    String fieldKey();

    /**
     * 同一字段的多个翻译资源声明。
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {

        /**
         * 按优先级排列的翻译资源声明。
         */
        StaskI18nField[] value();
    }
}
