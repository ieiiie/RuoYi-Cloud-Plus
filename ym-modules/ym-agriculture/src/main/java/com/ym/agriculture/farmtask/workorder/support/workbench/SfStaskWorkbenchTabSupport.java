package com.ym.agriculture.farmtask.workorder.support.workbench;

import com.ym.common.core.utils.StringUtils;

/**
 * 管理员与技术员共用的工作台分栏参数校验。
 */
public final class SfStaskWorkbenchTabSupport {

    private static final String PENDING = "pending";
    private static final String PROCESSING = "processing";
    private static final String COMPLETED_TODAY = "completed_today";

    private SfStaskWorkbenchTabSupport() {
    }

    /**
     * 规范化管理员体系分栏编码。
     *
     * @param tab 原始分栏编码
     * @return 小写规范编码
     */
    public static String normalizeManagerTab(String tab,
        com.ym.agriculture.shared.i18n.StaskMessageResolver messages) {
        if (StringUtils.isBlank(tab)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKBENCH_TAB_REQUIRED);
        }
        String normalized = tab.trim().toLowerCase();
        if (PENDING.equals(normalized) || PROCESSING.equals(normalized) || COMPLETED_TODAY.equals(normalized)) {
            return normalized;
        }
        throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_WORKBENCH_MANAGER_TAB_INVALID);
    }
}
