package com.ym.agriculture.shared.i18n.model;

/**
 * 待翻译中文文本。资源元数据仅用于运行时定位和响应字段映射，
 * 不写入租户级词条表。
 *
 * @param resourceType 资源类型
 * @param resourceId   资源主键
 * @param fieldKey     原字段或 JSON 文本路径
 * @param sourceText   中文原文
 */
public record I18nTextSource(String resourceType, Long resourceId, String fieldKey, String sourceText) {
}
