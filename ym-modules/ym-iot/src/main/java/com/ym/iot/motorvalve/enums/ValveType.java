package com.ym.iot.motorvalve.enums;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.motorvalve.domain.vo.ValveControlOptionVo;

import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 电动阀阀门类型枚举。
 *
 * <p>各类型阀门的快捷档位定义不同；直接角度控制统一允许 0-360 度整数：
 * <ul>
 *   <li>单通阀：0=全关，90=全开</li>
 *   <li>三通 L/T 型：0=全关，90=A全开，270=B全开</li>
 *   <li>五通阀：A 0°→60°，B 360°→300°，C 180°→240°，D 180°→120°</li>
 * </ul>
 *
 * @author ym-cloud
 */
@Getter
public enum ValveType {

    /** 单通阀：0=全关，90=全开 */
    SINGLE_PORT("single_port", "单通阀", new int[]{0, 90}),

    /** 三通 L 型：0=全关，90=A全开，270=B全开 */
    THREE_WAY_L("three_way_l", "三通L型", new int[]{0, 90, 270}),

    /** 三通 T 型：0=全关，90=A全开，270=B全开 */
    THREE_WAY_T("three_way_t", "三通T型", new int[]{0, 90, 270}),

    /** 五通阀：厂家确认的四通道百分比区间关键点 */
    FIVE_PORT("five_port", "五通阀", new int[]{0, 60, 120, 180, 240, 300, 360});

    /** 厂商协议允许的最大位置角度（度） */
    public static final int MAX_POSITION = 360;

    /**
     * 遥测开/关展示判定默认吸附容差（度）。
     */
    public static final double DEFAULT_POSITION_SNAP_TOLERANCE_DEGREES = 10.0;

    /**
     * 遥测吸附关阀位时的最大容差（度），严于开阀位。
     * 例如五通 176° 距 180° 关阀位 4°，不应视为「全关 180°」。
     */
    public static final double CLOSE_TELEMETRY_SNAP_TOLERANCE_DEGREES = 3.0;

    /** 类型编码，对应 iot_device 扩展字段或前端传参 */
    private final String code;

    /** 类型名称 */
    private final String label;

    /** 有效位置值列表（度） */
    private final int[] validPositions;

    ValveType(String code, String label, int[] validPositions) {
        this.code = code;
        this.label = label;
        this.validPositions = validPositions;
    }

    /**
     * 判断指定位置值是否为合法控制角度。
     *
     * @param position 目标位置（度）
     * @return true=有效
     */
    public boolean isValidPosition(int position) {
        return isValidAngle(position);
    }

    /**
     * 判断指定角度是否在厂商协议允许范围内。
     *
     * @param position 目标位置（度）
     * @return true=0-360 度整数
     */
    public static boolean isValidAngle(int position) {
        return position >= 0 && position <= MAX_POSITION;
    }

    /**
     * 判断是否为关阀位置。
     *
     * @param position 目标位置（度）
     * @return true=关阀
     */
    public boolean isClosedPosition(int position) {
        return switch (this) {
            case SINGLE_PORT, THREE_WAY_L, THREE_WAY_T -> position == 0;
            case FIVE_PORT -> position == 0 || position == 180 || position == 360;
        };
    }

    /**
     * 判断是否为开阀位置（协议第 4 节离散「全开」档位，精确匹配）。
     *
     * @param position 目标位置（度）
     * @return true=开阀
     */
    public boolean isOpenPosition(int position) {
        if (position < 0 || position > MAX_POSITION) {
            return false;
        }
        return switch (this) {
            case SINGLE_PORT -> position == 90;
            case THREE_WAY_L, THREE_WAY_T -> position == 90 || position == 270;
            case FIVE_PORT -> position == 60 || position == 120 || position == 240 || position == 300;
        };
    }

    /**
     * 遥测角度是否为开阀位：先吸附到最近离散档位，容差内再按阀型判定。
     *
     * @param angleDegrees       遥测角度（度，可带小数）
     * @param toleranceDegrees   与最近档位的最大圆心角差（度）
     * @return true=开阀；无法吸附或吸附到关阀位/中间角时为 false
     */
    public boolean isOpenAngleFromTelemetry(double angleDegrees, double toleranceDegrees) {
        Integer snapped = snapToNearestValidPosition(angleDegrees, toleranceDegrees);
        return snapped != null && isOpenPosition(snapped);
    }

    /**
     * 遥测角度是否为关阀位（离散全关档位，如五通 0°/180°/360°）。
     */
    public boolean isClosedAngleFromTelemetry(double angleDegrees, double toleranceDegrees) {
        Integer snapped = snapToNearestValidPosition(angleDegrees, toleranceDegrees);
        return snapped != null && isClosedPosition(snapped);
    }

    /**
     * 控阀会话归类：厂商协议明确关阀角为关，其余合法角度均按开阀记录。
     *
     * <p>该口径仅用于 {@code iot_motorvalve_valve_session} 开闭时长统计，不影响遥测展示吸附逻辑。
     *
     * @param angleDegrees     控阀目标角（度）
     * @param toleranceDegrees 保留兼容旧调用；会话归类不使用吸附容差
     * @return 开阀或关阀
     */
    public ValveSessionSide resolveSessionSide(double angleDegrees, double toleranceDegrees) {
        return resolveSessionSide(angleDegrees);
    }

    /**
     * 控阀会话归类：只要不是协议关阀角度，就按开阀记录。
     *
     * @param angleDegrees 控阀目标角（度）
     * @return 开阀或关阀
     */
    public ValveSessionSide resolveSessionSide(double angleDegrees) {
        if (Double.isNaN(angleDegrees) || Double.isInfinite(angleDegrees)) {
            return ValveSessionSide.CLOSE;
        }
        int position = (int) angleDegrees;
        if (angleDegrees != position || !isValidAngle(position)) {
            return ValveSessionSide.CLOSE;
        }
        return isClosedPosition(position) ? ValveSessionSide.CLOSE : ValveSessionSide.OPEN;
    }

    /**
     * 圆心角最近的离散档位（不做容差截断）。
     *
     * @param angleDegrees 角度（度）
     * @return 最近档位；非法角度返回 null
     */
    public Integer findNearestPreset(double angleDegrees) {
        if (Double.isNaN(angleDegrees) || Double.isInfinite(angleDegrees)) {
            return null;
        }
        double normalized = normalizeAngle(angleDegrees);
        int nearest = validPositions[0];
        double minDistance = circularDistance(normalized, nearest);
        for (int position : validPositions) {
            double distance = circularDistance(normalized, position);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = position;
            }
        }
        return nearest;
    }

    /**
     * 将遥测角度吸附到本阀型最近离散档位。
     *
     * @param angleDegrees     遥测角度（度）
     * @param toleranceDegrees 容差（度）
     * @return 吸附后的档位角度；超出容差返回 null
     */
    public Integer snapToNearestValidPosition(double angleDegrees, double toleranceDegrees) {
        if (Double.isNaN(angleDegrees) || Double.isInfinite(angleDegrees) || toleranceDegrees < 0) {
            return null;
        }
        double normalized = normalizeAngle(angleDegrees);
        int nearest = validPositions[0];
        double minDistance = circularDistance(normalized, nearest);
        for (int position : validPositions) {
            double distance = circularDistance(normalized, position);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = position;
            }
        }
        double allowedTolerance = effectiveSnapTolerance(nearest, toleranceDegrees);
        return minDistance <= allowedTolerance ? nearest : null;
    }

    private double effectiveSnapTolerance(int nearestPreset, double toleranceDegrees) {
        if (isClosedPosition(nearestPreset)) {
            return Math.min(toleranceDegrees, CLOSE_TELEMETRY_SNAP_TOLERANCE_DEGREES);
        }
        return toleranceDegrees;
    }

    private static double normalizeAngle(double angleDegrees) {
        double normalized = angleDegrees % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }

    private static double circularDistance(double angleDegrees, int presetDegrees) {
        double diff = Math.abs(angleDegrees - presetDegrees);
        return Math.min(diff, 360.0 - diff);
    }

    /**
     * 按编码解析阀门类型，未知编码默认单通阀。
     *
     * @param code 类型编码
     * @return 阀门类型
     */
    public static ValveType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return SINGLE_PORT;
        }
        ValveType type = findByCodeOrAlias(code);
        return type != null ? type : SINGLE_PORT;
    }

    /**
     * 按编码严格解析阀门类型，未知编码直接拒绝。
     *
     * @param code 类型编码，支持 single_valve / five_way 兼容别名
     * @return 阀门类型
     */
    public static ValveType fromStrictCode(String code) {
        if (code == null || code.isBlank()) {
            throw new ServiceException("阀门类型不能为空");
        }
        ValveType type = findByCodeOrAlias(code);
        if (type == null) {
            throw new ServiceException("不支持的阀门类型: " + code);
        }
        return type;
    }

    private static ValveType findByCodeOrAlias(String code) {
        String normalized = code.trim();
        for (ValveType type : values()) {
            if (type.code.equalsIgnoreCase(normalized)) {
                return type;
            }
        }
        if ("single_valve".equalsIgnoreCase(normalized)) {
            return SINGLE_PORT;
        }
        if ("five_way".equalsIgnoreCase(normalized)) {
            return FIVE_PORT;
        }
        return null;
    }

    /** 4G 直连 / 网关控阀可选档位（与 {@link #positionForAction(String)} 一致）。 */
    public List<ValveControlOptionVo> listControlOptions() {
        List<ValveControlOptionVo> options = new ArrayList<>();
        switch (this) {
            case SINGLE_PORT -> {
                options.add(option("close", "全关", 0));
                options.add(option("open", "全开", 90));
            }
            case THREE_WAY_L, THREE_WAY_T -> {
                options.add(option("close", "全关", 0));
                options.add(option("open_a", "A口全开", 90));
                options.add(option("open_b", "B口全开", 270));
            }
            case FIVE_PORT -> {
                options.add(option("close", "全关", 0));
                options.add(option("open_a", "A全开", 60));
                options.add(option("open_b", "B全开", 300));
                options.add(option("open_c", "C全开", 240));
                options.add(option("open_d", "D全开", 120));
            }
            default -> {
                // no-op
            }
        }
        return options;
    }

    /**
     * 将控制动作解析为协议位置（度）。
     *
     * @param action 如 close、open_a
     * @return 位置（度）
     */
    public int positionForAction(String action) {
        if (action == null || action.isBlank()) {
            throw new ServiceException("控制动作 action 不能为空");
        }
        String code = action.trim().toLowerCase();
        for (ValveControlOptionVo opt : listControlOptions()) {
            if (opt.getAction().equalsIgnoreCase(code)) {
                return opt.getPosition();
            }
        }
        throw new ServiceException("阀门类型 " + label + " 不支持控制动作: " + action
            + "，请调用 GET /motorvalve/{deviceId}/control-profile 获取合法 action");
    }

    private static ValveControlOptionVo option(String action, String label, int position) {
        return new ValveControlOptionVo(action, label, position);
    }

    // ────────── 百分比控制通道区间（V1.0 统一线性插值算法） ──────────

    /**
     * 通道百分比控制的角度区间。
     *
     * <p>统一公式 {@code θ = start + (end - start) × P / 100}：
     * <ul>
     *   <li>{@code start}：该通道关阀角度（P=0% 时输出）</li>
     *   <li>{@code end}：该通道全开角度（P=100% 时输出）</li>
     * </ul>
     *
     * @param start 关阀角度（度）
     * @param end   全开角度（度）
     */
    public record ChannelAngle(int start, int end) {

        /**
         * 按百分比线性插值计算目标角度。
         *
         * @param percent 开度百分比，0-100
         * @return 目标角度（度，四舍五入到整数）
         */
        public int interpolate(int percent) {
            return (int) Math.round(start + (end - start) * (percent / 100.0));
        }
    }

    /**
     * 获取该阀型支持的全部通道键名（单通阀返回空集合）。
     *
     * @return 通道键集合，如 {"A","B"} 或 {"A","B","C","D"}；单通阀为空
     */
    public Set<String> supportedChannels() {
        return channelAngleTable().keySet();
    }

    /**
     * 查询指定通道的角度区间（关→开）。
     *
     * @param channel 通道键名 A/B/C/D（大小写不敏感），单通阀应传 null 或空
     * @return 角度区间；单通阀恒为 (0, 90)
     * @throws ServiceException 通道不存在于当前阀型
     */
    public ChannelAngle channelAngle(String channel) {
        if (this == SINGLE_PORT) {
            // 单通阀无通道概念，统一 0→90
            return new ChannelAngle(0, 90);
        }
        String key = normalizeChannelKey(channel);
        ChannelAngle angle = channelAngleTable().get(key);
        if (angle == null) {
            throw new ServiceException("不支持的通道 " + channel + "，当前阀型仅支持 "
                + String.join("/", channelAngleTable().keySet()));
        }
        return angle;
    }

    /** 归一化通道键名为大写 A/B/C/D */
    private static String normalizeChannelKey(String channel) {
        if (channel == null || channel.isBlank()) {
            throw new ServiceException("通道名称不能为空");
        }
        return channel.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * 各阀型通道→角度区间表。
     *
     * <p>区间定义（start=关, end=全开）：
     * <ul>
     *   <li>三通 A：0°→90°（A全开）；三通 B：90°→270°（B全开）</li>
     *   <li>五通 A：0°→60°（A全开）；五通 B：360°→300°（B全开）；
     *       五通 C：180°→240°（C全开）；五通 D：180°→120°（D全开）</li>
     * </ul>
     */
    private Map<String, ChannelAngle> channelAngleTable() {
        Map<String, ChannelAngle> table = new LinkedHashMap<>();
        switch (this) {
            case SINGLE_PORT -> {
                // 单通阀不进入通道表，channelAngle 方法单独处理
            }
            case THREE_WAY_L, THREE_WAY_T -> {
                table.put("A", new ChannelAngle(0, 90));
                table.put("B", new ChannelAngle(90, 270));
            }
            case FIVE_PORT -> {
                table.put("A", new ChannelAngle(0, 60));
                table.put("B", new ChannelAngle(360, 300));
                table.put("C", new ChannelAngle(180, 240));
                table.put("D", new ChannelAngle(180, 120));
            }
            default -> {
                // no-op
            }
        }
        return table;
    }
}
