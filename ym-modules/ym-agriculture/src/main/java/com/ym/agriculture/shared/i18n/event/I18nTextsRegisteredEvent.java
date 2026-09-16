package com.ym.agriculture.shared.i18n.event;

/**
 * 翻译待办已在业务事务中登记或重新入队。事件不携带业务原文，只用于提交后唤醒 Worker。
 *
 * @param tenantId 待处理资源所属租户
 */
public record I18nTextsRegisteredEvent(String tenantId) {
}
