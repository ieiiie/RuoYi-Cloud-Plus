package com.ym.agriculture.farming.solarterms.support;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 二十四节气编码与中文名映射（顺序从立春到大寒）。
 */
public final class SolarTermCodes {

    public static final String ALGORITHM_VERSION = "lunar-1.7.7";

    private static final Map<String, String> CODE_TO_NAME;
    private static final Map<String, String> NAME_TO_CODE;

    static {
        Map<String, String> codeToName = new LinkedHashMap<>();
        codeToName.put("lichun", "立春");
        codeToName.put("yushui", "雨水");
        codeToName.put("jingzhe", "惊蛰");
        codeToName.put("chunfen", "春分");
        codeToName.put("qingming", "清明");
        codeToName.put("guyu", "谷雨");
        codeToName.put("lixia", "立夏");
        codeToName.put("xiaoman", "小满");
        codeToName.put("mangzhong", "芒种");
        codeToName.put("xiazhi", "夏至");
        codeToName.put("xiaoshu", "小暑");
        codeToName.put("dashu", "大暑");
        codeToName.put("liqiu", "立秋");
        codeToName.put("chushu", "处暑");
        codeToName.put("bailu", "白露");
        codeToName.put("qiufen", "秋分");
        codeToName.put("hanlu", "寒露");
        codeToName.put("shuangjiang", "霜降");
        codeToName.put("lidong", "立冬");
        codeToName.put("xiaoxue", "小雪");
        codeToName.put("daxue", "大雪");
        codeToName.put("dongzhi", "冬至");
        codeToName.put("xiaohan", "小寒");
        codeToName.put("dahan", "大寒");
        CODE_TO_NAME = Collections.unmodifiableMap(codeToName);

        Map<String, String> nameToCode = new LinkedHashMap<>();
        codeToName.forEach((code, name) -> nameToCode.put(name, code));
        NAME_TO_CODE = Collections.unmodifiableMap(nameToCode);
    }

    private SolarTermCodes() {
    }

    public static Map<String, String> codeToName() {
        return CODE_TO_NAME;
    }

    public static String codeOfName(String termName) {
        return NAME_TO_CODE.get(termName);
    }

    public static String nameOfCode(String termCode) {
        return CODE_TO_NAME.get(termCode);
    }
}
