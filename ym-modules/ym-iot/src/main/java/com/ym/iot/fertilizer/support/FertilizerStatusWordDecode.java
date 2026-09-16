package com.ym.iot.fertilizer.support;

import com.ym.iot.fertilizer.enums.FertilizerStatus1Bit;
import com.ym.iot.fertilizer.enums.FertilizerStatus2Bit;
import com.ym.iot.fertilizer.enums.FertilizerStatus3Bit;
import com.ym.iot.fertilizer.enums.FertilizerStatus4Bit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将 0x0B 实时帧四个状态字解码为有序 Map，键为枚举名小写（与 REST JSON 约定一致）。
 *
 * @author ym-cloud
 */
public final class FertilizerStatusWordDecode {

    private FertilizerStatusWordDecode() {}

    public static Map<String, Boolean> decodeS1(int raw) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        for (FertilizerStatus1Bit b : FertilizerStatus1Bit.values()) {
            m.put(b.name().toLowerCase(), (raw & b.getMask()) != 0);
        }
        return m;
    }

    public static Map<String, Boolean> decodeS2(int raw) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        for (FertilizerStatus2Bit b : FertilizerStatus2Bit.values()) {
            m.put(b.name().toLowerCase(), (raw & b.getMask()) != 0);
        }
        return m;
    }

    public static Map<String, Boolean> decodeS3(int raw) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        for (FertilizerStatus3Bit b : FertilizerStatus3Bit.values()) {
            m.put(b.name().toLowerCase(), (raw & b.getMask()) != 0);
        }
        return m;
    }

    public static Map<String, Boolean> decodeS4(int raw) {
        Map<String, Boolean> m = new LinkedHashMap<>();
        for (FertilizerStatus4Bit b : FertilizerStatus4Bit.values()) {
            m.put(b.name().toLowerCase(), (raw & b.getMask()) != 0);
        }
        return m;
    }
}
