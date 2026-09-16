package com.ym.iot.wvp.enums;

import com.ym.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * WVP 前端云台 {@code /api/front-end/ptz/...} 的 {@code command} 取值。
 */
public enum WvpFrontEndPtzCommand {
    UP("up"),
    DOWN("down"),
    LEFT("left"),
    RIGHT("right"),
    UPLEFT("upleft"),
    UPRIGHT("upright"),
    DOWNLEFT("downleft"),
    DOWNRIGHT("downright"),
    ZOOMIN("zoomin"),
    ZOOMOUT("zoomout"),
    STOP("stop");

    private final String apiValue;

    WvpFrontEndPtzCommand(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    public static WvpFrontEndPtzCommand fromApiValue(String command) {
        if (StringUtils.isBlank(command)) {
            return null;
        }
        String t = command.trim();
        for (WvpFrontEndPtzCommand c : values()) {
            if (c.apiValue.equalsIgnoreCase(t)) {
                return c;
            }
        }
        return null;
    }

    /**
     * 非法指令提示中的允许取值列表。
     */
    public static String allowedValuesHint() {
        return Arrays.stream(values())
            .map(WvpFrontEndPtzCommand::getApiValue)
            .collect(Collectors.joining(", "));
    }
}
