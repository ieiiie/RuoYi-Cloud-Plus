package com.ym.iot.motorvalve.enums;

/**
 * 开阀会话归类：开阀或关阀（仅会话层，与展示层严格吸附口径解耦）。
 */
public enum ValveSessionSide {

    /** 开阀：创建 OPEN 会话 */
    OPEN,

    /** 关阀：结束 OPEN 或插入 CLOSED 记录 */
    CLOSE
}
