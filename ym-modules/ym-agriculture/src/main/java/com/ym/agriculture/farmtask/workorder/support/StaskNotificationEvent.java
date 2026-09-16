package com.ym.agriculture.farmtask.workorder.support;

import com.ym.agriculture.shared.i18n.BilingualContent;

/**
 * stask 业务提交后的通知事件。
 *
 * @param scene 通知场景
 * @param targetId 目标员工 ID
 * @param content 双语正文
 */
public record StaskNotificationEvent(String scene, Long targetId, BilingualContent content) {
}
