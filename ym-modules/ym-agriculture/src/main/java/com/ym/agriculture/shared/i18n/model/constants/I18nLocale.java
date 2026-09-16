package com.ym.agriculture.shared.i18n.model.constants;

/**
 * 智慧农业支持的业务展示语言。
 */
public final class I18nLocale {

    /** 简体中文。 */
    public static final String ZH_CN = "zh-CN";

    /** 维吾尔文。 */
    public static final String UG_CN = "ug-CN";

    private I18nLocale() {
    }

    /**
     * 将请求语言规范为当前支持的语言。
     *
     * @param language 请求头语言
     * @return 规范语言；未知语言回退中文
     */
    public static String normalize(String language) {
        if (language == null) {
            return ZH_CN;
        }
        String normalized = language.replace('_', '-').trim();
        if (UG_CN.equalsIgnoreCase(normalized)) {
            return UG_CN;
        }
        return ZH_CN;
    }

    /**
     * 判断是否为维吾尔文。
     *
     * @param language 请求头语言
     * @return 是维文时返回 true
     */
    public static boolean isUyghur(String language) {
        return UG_CN.equals(normalize(language));
    }
}
