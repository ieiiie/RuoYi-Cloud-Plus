package com.ym.agriculture.farming.weatheralert.support;

import com.ym.common.core.utils.StringUtils;

/**
 * 从预警 ID / 标题 / 图标 URL 解析类型、等级与区划。
 * <p>alertid 形如 {@code 33040041600000_20260722154751}，前 6 位为 adcode。
 */
public final class NmcAlertParser {

    private NmcAlertParser() {
    }

    public static String extractAreaAdcode(String warningId) {
        if (StringUtils.isBlank(warningId)) {
            return null;
        }
        String id = warningId.trim();
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < id.length() && digits.length() < 6; i++) {
            char c = id.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                break;
            }
        }
        return digits.length() == 6 ? digits.toString() : null;
    }

    public static RegionCodes toRegionCodes(String areaAdcode) {
        String ad = pad6(areaAdcode);
        if (ad == null) {
            return new RegionCodes(null, null, null);
        }
        String province = ad.substring(0, 2) + "0000";
        if (ad.endsWith("0000")) {
            return new RegionCodes(province, null, null);
        }
        if (ad.endsWith("00")) {
            return new RegionCodes(province, ad, null);
        }
        String city = ad.substring(0, 4) + "00";
        return new RegionCodes(province, city, ad);
    }

    public static LevelInfo parseLevelFromTitle(String title) {
        if (StringUtils.isBlank(title)) {
            return new LevelInfo(null, null);
        }
        if (title.contains("红色")) {
            return new LevelInfo("RED", "红色");
        }
        if (title.contains("橙色")) {
            return new LevelInfo("ORANGE", "橙色");
        }
        if (title.contains("黄色")) {
            return new LevelInfo("YELLOW", "黄色");
        }
        if (title.contains("蓝色")) {
            return new LevelInfo("BLUE", "蓝色");
        }
        if (title.contains("白色")) {
            return new LevelInfo("WHITE", "白色");
        }
        return new LevelInfo(null, null);
    }

    public static TypeInfo parseTypeFromTitle(String title) {
        if (StringUtils.isBlank(title)) {
            return new TypeInfo(null, null);
        }
        String[] types = {
            "台风", "暴雨", "暴雪", "寒潮", "大风", "沙尘暴", "高温", "干旱",
            "雷电", "冰雹", "霜冻", "大雾", "霾", "道路结冰", "雷暴大风",
            "强对流", "寒冷", "灰霾", "雷雨大风", "森林火险", "地质灾害",
            "山洪", "干热风", "低温", "冰冻", "重污染"
        };
        for (String type : types) {
            if (title.contains(type)) {
                return new TypeInfo(type, type);
            }
        }
        return new TypeInfo(null, null);
    }

    /**
     * 图标名形如 p0012003.png：后四位中末位常对应蓝黄橙红（4/3/2/1）。
     */
    public static LevelInfo parseLevelFromPic(String picUrl) {
        if (StringUtils.isBlank(picUrl)) {
            return new LevelInfo(null, null);
        }
        int slash = picUrl.lastIndexOf('/');
        String file = slash >= 0 ? picUrl.substring(slash + 1) : picUrl;
        int dot = file.lastIndexOf('.');
        String name = dot > 0 ? file.substring(0, dot) : file;
        if (name.length() < 1) {
            return new LevelInfo(null, null);
        }
        char last = name.charAt(name.length() - 1);
        return switch (last) {
            case '1' -> new LevelInfo("RED", "红色");
            case '2' -> new LevelInfo("ORANGE", "橙色");
            case '3' -> new LevelInfo("YELLOW", "黄色");
            case '4' -> new LevelInfo("BLUE", "蓝色");
            default -> new LevelInfo(null, null);
        };
    }

    public static String guessPublisher(String title) {
        if (StringUtils.isBlank(title)) {
            return null;
        }
        int idx = title.indexOf("发布");
        if (idx > 0) {
            return title.substring(0, idx);
        }
        return null;
    }

    public static String guessAreaText(String title) {
        String publisher = guessPublisher(title);
        if (StringUtils.isBlank(publisher)) {
            return null;
        }
        return publisher.replace("气象台", "").replace("中心气象台", "");
    }

    public static int levelRank(String levelCode) {
        if (levelCode == null) {
            return 0;
        }
        return switch (levelCode) {
            case "RED" -> 4;
            case "ORANGE" -> 3;
            case "YELLOW" -> 2;
            case "BLUE" -> 1;
            case "WHITE" -> 1;
            default -> 0;
        };
    }

    public static String pad6(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String s = raw.trim();
        if (!s.chars().allMatch(Character::isDigit)) {
            return s;
        }
        if (s.length() > 6) {
            return s.substring(0, 6);
        }
        while (s.length() < 6) {
            s = "0" + s;
        }
        return s;
    }

    public record RegionCodes(String provinceCode, String cityCode, String districtCode) {
    }

    public record LevelInfo(String code, String name) {
    }

    public record TypeInfo(String code, String name) {
    }
}
