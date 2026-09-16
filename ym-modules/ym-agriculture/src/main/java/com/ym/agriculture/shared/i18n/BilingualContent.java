package com.ym.agriculture.shared.i18n;

import com.ym.common.core.utils.StringUtils;

/**
 * 一份相互对应的中文和维吾尔文内容。
 *
 * @param zhCn 中文内容
 * @param ugCn 维吾尔文内容；缺失时回退中文
 */
public record BilingualContent(String zhCn, String ugCn) {

    public BilingualContent {
        zhCn = StringUtils.blankToDefault(zhCn, "");
        ugCn = StringUtils.blankToDefault(ugCn, zhCn);
    }

    /**
     * 以中文段在前、维文段在后的形式渲染完整正文。
     *
     * @return 双语正文
     */
    public String render() {
        return zhCn + "\n" + ugCn;
    }
}
