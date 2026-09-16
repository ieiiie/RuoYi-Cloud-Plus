package com.ym.agriculture.shared.i18n.model;

/**
 * 不依赖业务主键的类型化译文查找条件。资源和字段仅用于运行时映射，
 * 持久化查询只使用租户、语言和中文原文摘要。
 *
 * @param resourceType 资源类型
 * @param fieldKey 字段键
 * @param sourceText 中文原文
 */
public record I18nTextLookup(String resourceType, String fieldKey, String sourceText) {
}
