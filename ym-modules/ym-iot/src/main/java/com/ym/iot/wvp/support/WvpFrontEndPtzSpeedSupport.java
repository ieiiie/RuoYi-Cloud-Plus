package com.ym.iot.wvp.support;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.wvp.enums.WvpFrontEndPtzCommand;

/**
 * WVP 前端云台速度规范化，与 WVP Web {@code controSpeed=30} 及 {@code PtzController} 行为对齐。
 */
public final class WvpFrontEndPtzSpeedSupport {

    /** WVP Web 默认水平速度（约 UI 30% 对应 76） */
    public static final int DEFAULT_HORIZON_SPEED = 76;
    /** WVP Web 默认垂直速度 */
    public static final int DEFAULT_VERTICAL_SPEED = 76;
    /** WVP Web 默认变倍速度（0-15） */
    public static final int DEFAULT_ZOOM_SPEED = 4;

    private WvpFrontEndPtzSpeedSupport() {
    }

    /**
     * 规范化后的三轴速度。
     *
     * @param horizonSpeed 水平速度，0-255
     * @param verticalSpeed 垂直速度，0-255
     * @param zoomSpeed 变倍速度，0-15
     */
    public record ResolvedSpeeds(int horizonSpeed, int verticalSpeed, int zoomSpeed) {
    }

    /**
     * 按指令与入参解析速度：{@code stop} 强制 0/0/0；未传参时使用 WVP Web 默认值。
     */
    public static ResolvedSpeeds resolve(
        WvpFrontEndPtzCommand command,
        Integer horizonSpeed,
        Integer verticalSpeed,
        Integer zoomSpeed) {
        if (command == WvpFrontEndPtzCommand.STOP) {
            return new ResolvedSpeeds(0, 0, 0);
        }
        int horizon = horizonSpeed != null ? validatePanTilt(horizonSpeed, "horizonSpeed") : DEFAULT_HORIZON_SPEED;
        int vertical = verticalSpeed != null ? validatePanTilt(verticalSpeed, "verticalSpeed") : DEFAULT_VERTICAL_SPEED;
        int zoom = zoomSpeed != null ? validateZoom(zoomSpeed) : DEFAULT_ZOOM_SPEED;
        return new ResolvedSpeeds(horizon, vertical, zoom);
    }

    private static int validatePanTilt(int speed, String name) {
        if (speed < 0 || speed > 255) {
            throw new ServiceException(name + " 须为 0-255 的整数");
        }
        return speed;
    }

    private static int validateZoom(int speed) {
        if (speed < 0 || speed > 15) {
            throw new ServiceException("zoomSpeed 须为 0-15 的整数");
        }
        return speed;
    }
}
