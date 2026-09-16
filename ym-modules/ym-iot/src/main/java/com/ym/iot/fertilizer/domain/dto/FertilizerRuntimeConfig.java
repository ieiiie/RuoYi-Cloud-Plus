package com.ym.iot.fertilizer.domain.dto;

/**
 * 施肥机运行配置。
 *
 * @param testModeEnabled 测试模式是否开启
 */
public record FertilizerRuntimeConfig(boolean testModeEnabled) {
}
