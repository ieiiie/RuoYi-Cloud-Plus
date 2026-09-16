package com.ym.agriculture.farmtask.workorder.model.constants;

import com.ym.common.core.utils.StringUtils;

/**
 * stask 组长分配模式常量。
 */
public interface StaskLeaderAssignMode {

    /**
     * 按后台“大棚 + 农事项”配置自动分配组长。
     */
    String AUTO = "AUTO";

    /**
     * 当前任务包农事项临时手动指派组长。
     */
    String MANUAL = "MANUAL";

    /**
     * 空值按自动分配处理。
     */
    static String normalize(String mode) {
        return StringUtils.isBlank(mode) ? AUTO : mode.trim().toUpperCase();
    }

    /**
     * 是否为支持的分配模式。
     */
    static boolean isSupported(String mode) {
        return AUTO.equals(mode) || MANUAL.equals(mode);
    }
}
