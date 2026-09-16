package com.ym.iot.motorvalve.support;

import cn.hutool.core.util.StrUtil;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.motorvalve.domain.bo.ValvePercentControlBo;
import com.ym.iot.motorvalve.enums.ValveType;
import com.ym.iot.motorvalve.enums.ValveType.ChannelAngle;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 电动阀百分比控制角度计算器（V1.0 统一线性插值算法）。
 *
 * <p>统一公式：{@code θ = start + (end - start) × P / 100}，其中 {@code start}=通道关阀角度，
 * {@code end}=通道全开角度，{@code P}=该通道开度百分比（0-100）。
 *
 * <p>核心特征：
 * <ul>
 *   <li>单通道选择：每次只控制一个通道（A/B/C/D），不再使用多通道 Winner-Take-All</li>
 *   <li>单通阀：无通道概念，直接 0→90 线性</li>
 *   <li>三通 A：0°→90°（A全开）；三通 B：90°→270°（B全开）</li>
 *   <li>五通 A：0°→60°（A全开）；五通 B：360°→300°（B全开）；
 *       五通 C：180°→240°（C全开）；五通 D：180°→120°（D全开）</li>
 * </ul>
 */
public final class ValvePercentControlCalculator {

    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 100;

    /** 反向换算开/关阀位吸附容差（度） */
    private static final int REVERSE_SNAP_TOLERANCE = 2;

    private ValvePercentControlCalculator() {
    }

    /**
     * 将请求阀型编码规范化为平台内部编码。
     *
     * @param requestValveType 请求传入的阀型编码，可为空
     * @return 平台内部阀型编码；未传时返回 null
     */
    public static String normalizeRequestValveTypeCode(String requestValveType) {
        if (StrUtil.isBlank(requestValveType)) {
            return null;
        }
        return ValveType.fromStrictCode(requestValveType).getCode();
    }

    /**
     * 根据阀门类型和百分比请求计算厂商协议角度（统一线性插值）。
     *
     * @param valveType 阀门类型
     * @param bo        百分比控制参数（单通读 percent；三通/五通读 channel + percent）
     * @return 协议角度，单位：度
     */
    public static int calculateAngle(ValveType valveType, ValvePercentControlBo bo) {
        if (valveType == null) {
            throw new ServiceException("阀门类型不能为空");
        }
        if (bo == null) {
            throw new ServiceException("百分比控制参数不能为空");
        }
        int percent = resolveRequiredPercent(bo, valveType);
        assertValidPercent(percent, "percent");
        if (valveType == ValveType.SINGLE_PORT) {
            // 单通阀：0→90 线性
            return new ChannelAngle(0, 90).interpolate(percent);
        }
        // 三通/五通：按选中通道区间线性插值
        String channel = resolveRequiredChannel(bo, valveType);
        return valveType.channelAngle(channel).interpolate(percent);
    }

    /**
     * 计算角度对应的状态文案（用于结果展示与滑块刻度标注）。
     *
     * <p>规则：
     * <ul>
     *   <li>角度等于关阀位 → 「{start}° 全关」</li>
     *   <li>角度等于全开位 → 「{end}° {通道}口全开」（单通阀为「{end}° 全开」）</li>
     *   <li>中间档 → 「{θ}°」（仅角度，不命名状态）</li>
     * </ul>
     *
     * @param valveType 阀门类型
     * @param channel   通道（单通阀可为 null）
     * @param angle     计算得到的目标角度
     * @return 状态文案
     */
    public static String angleLabel(ValveType valveType, String channel, int angle) {
        if (valveType == null) {
            return angle + "°";
        }
        if (valveType == ValveType.SINGLE_PORT) {
            ChannelAngle range = new ChannelAngle(0, 90);
            if (angle == range.start()) {
                return "0° 全关";
            }
            if (angle == range.end()) {
                return "90° 全开";
            }
            return angle + "°";
        }
        String normalized = normalizeChannelKey(channel);
        ChannelAngle range = valveType.channelAngle(normalized);
        String prefix = normalized + "口";
        if (angle == range.start()) {
            return range.start() + "° 全关";
        }
        if (angle == range.end()) {
            return range.end() + "° " + prefix + "全开";
        }
        return angle + "°";
    }

    /**
     * 解析请求百分比：单通阀读 {@code percent}；三通/五通读 {@code percent}（V1.0 选中通道开度）。
     *
     * @param bo        百分比控制参数
     * @param valveType 阀门类型
     * @return 百分比
     * @throws ServiceException 百分比为空
     */
    private static int resolveRequiredPercent(ValvePercentControlBo bo, ValveType valveType) {
        Integer percent = bo.getPercent();
        if (percent == null) {
            String hint = valveType == ValveType.SINGLE_PORT
                ? "单通阀百分比控制时 percent 不能为空"
                : "三通/五通百分比控制时 percent（选中通道开度）不能为空";
            throw new ServiceException(hint);
        }
        return percent;
    }

    /**
     * 解析三通/五通请求的选中通道。
     *
     * @param bo        百分比控制参数
     * @param valveType 阀门类型（非单通）
     * @return 归一化通道键名 A/B/C/D
     * @throws ServiceException 通道为空或非法
     */
    private static String resolveRequiredChannel(ValvePercentControlBo bo, ValveType valveType) {
        String channel = bo.getChannel();
        if (StrUtil.isBlank(channel)) {
            throw new ServiceException(valveType.getLabel() + " 百分比控制时 channel（A/B"
                + (valveType == ValveType.FIVE_PORT ? "/C/D" : "") + "）不能为空");
        }
        String normalized = normalizeChannelKey(channel);
        if (!valveType.supportedChannels().contains(normalized)) {
            throw new ServiceException("不支持的通道 " + channel + "，当前阀型仅支持 "
                + String.join("/", valveType.supportedChannels()));
        }
        return normalized;
    }

    /** 归一化通道键名为大写 A/B/C/D */
    private static String normalizeChannelKey(String channel) {
        String normalized = StrUtil.trimToEmpty(channel).toUpperCase(Locale.ROOT);
        if (StrUtil.isBlank(normalized)) {
            throw new ServiceException("通道名称不能为空");
        }
        return normalized;
    }

    private static void assertValidPercent(Integer percent, String fieldName) {
        if (percent < MIN_PERCENT || percent > MAX_PERCENT) {
            throw new ServiceException(fieldName + " 百分比无效，有效范围为 0-100");
        }
    }

    // ──────── 反向换算：角度 → 百分比 ────────

    /**
     * 从遥测角度反向计算百分比状态（V1.0 区间线性反算）。
     *
     * <p>归属规则：角度落入某通道区间（含关阀位与全开位端点，支持 B 口 360→300 反向区间）
     * 时按线性反算百分比；落入两区间夹缝时按「圆心距最近通道」归属。
     *
     * <ul>
     *   <li>单通阀：{@code percent = angle × 100 / 90}（0-90° 线性）</li>
     *   <li>三通/五通：找到归属通道，{@code percent = (angle - start) / (end - start) × 100}；
     *       若角度等于关阀位 → percent=0、全关</li>
     * </ul>
     *
     * @param valveType 阀门类型
     * @param angle     遥测角度（度）
     * @return 反向换算结果
     */
    public static ReversePercentResult reverseCalculatePercent(ValveType valveType, int angle) {
        if (valveType == null) {
            throw new ServiceException("阀门类型不能为空");
        }
        if (valveType == ValveType.SINGLE_PORT) {
            return reverseSinglePort(angle);
        }
        return reverseMultiChannel(valveType, angle);
    }

    /** 单通阀反向：角度 → 百分比（0→90 线性，360°≈0°） */
    private static ReversePercentResult reverseSinglePort(int angle) {
        if (isCloseTo(angle, 0, REVERSE_SNAP_TOLERANCE)) {
            return new ReversePercentResult(0, null, 0, null);
        }
        int percent = Math.round(Math.max(0, Math.min(MAX_PERCENT, angle * 100.0f / 90)));
        return new ReversePercentResult(percent, null, 0, null);
    }

    /**
     * 三通/五通反向：按圆心距归属通道区间，再线性反算百分比。
     */
    private static ReversePercentResult reverseMultiChannel(ValveType valveType, int angle) {
        Map<String, Integer> emptyChannels = emptyChannels(valveType);
        // 先匹配关阀位（三通 0°、五通 0/180/360°）→ 全关
        if (valveType.isClosedAngleFromTelemetry(angle, REVERSE_SNAP_TOLERANCE)) {
            return new ReversePercentResult(0, "", 0, emptyChannels);
        }
        String owner = null;
        double ownerDistance = Double.MAX_VALUE;
        ChannelAngle ownerRange = null;
        int normalizedAngle = Math.floorMod(angle, 360);
        for (String ch : valveType.supportedChannels()) {
            ChannelAngle range = valveType.channelAngle(ch);
            int low = Math.min(range.start(), range.end());
            int high = Math.max(range.start(), range.end());
            if (normalizedAngle >= low && normalizedAngle <= high) {
                owner = ch;
                ownerRange = range;
                ownerDistance = 0;
                break;
            }
            double distance = Math.min(circularDistance(normalizedAngle, range.start()),
                circularDistance(normalizedAngle, range.end()));
            if (distance < ownerDistance) {
                ownerDistance = distance;
                owner = ch;
                ownerRange = range;
            }
        }
        if (owner == null || ownerRange == null) {
            return new ReversePercentResult(0, "", 0, emptyChannels);
        }
        // 线性反算百分比：percent = (angle - start) / (end - start) × 100，支持 start > end 的反向区间
        int span = ownerRange.end() - ownerRange.start();
        int percent;
        if (span == 0) {
            percent = 100;
        } else {
            double ratio = (normalizedAngle - ownerRange.start()) / (double) span;
            ratio = Math.max(0, Math.min(1, ratio));
            percent = (int) Math.round(ratio * 100);
        }
        Map<String, Integer> channels = emptyChannels(valveType);
        channels.put(owner, percent);
        return new ReversePercentResult(percent, owner, percent, channels);
    }

    /** 构造全 0 的通道映射（三通 {A:0,B:0}，五通 {A:0,B:0,C:0,D:0}） */
    private static Map<String, Integer> emptyChannels(ValveType valveType) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String ch : valveType.supportedChannels()) {
            map.put(ch, 0);
        }
        return map;
    }

    /** 判断角度是否接近目标值（环形距离，360° ≈ 0°） */
    private static boolean isCloseTo(int angle, int target, int tolerance) {
        return circularDistance(angle, target) < tolerance;
    }

    private static double circularDistance(int angle, int target) {
        double diff = Math.abs((double) angle - target);
        return Math.min(diff, 360.0 - diff);
    }

    /**
     * 反向换算结果：从遥测角度推导出的百分比状态。
     *
     * @param percent        选中通道的开度百分比（0-100）；单通阀为整体百分比
     * @param selectedOutlet 当前选中的出口方向（A/B/C/D），空字符串或 null 表示全关/单通
     * @param outletPercent  选中出口的开度百分比（与 percent 相同，保留以兼容旧调用）
     * @param channels       完整通道百分比映射，单通阀为 null
     */
    public record ReversePercentResult(
        int percent,
        String selectedOutlet,
        int outletPercent,
        Map<String, Integer> channels
    ) {}
}
