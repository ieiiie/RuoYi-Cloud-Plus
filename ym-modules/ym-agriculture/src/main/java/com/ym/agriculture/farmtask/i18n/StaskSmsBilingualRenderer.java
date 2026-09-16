package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.BilingualContent;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * stask 短信双语正文渲染器。
 *
 * <p>按 Unicode code point 计算长度，分别保留中文和维文预算，避免从尾部截断时只剩中文。</p>
 */
@Component
public class StaskSmsBilingualRenderer {

    private final int maxCodePoints;

    public StaskSmsBilingualRenderer(
        @Value("${ym.stask.notify.content-max-length:160}") int maxCodePoints) {
        this.maxCodePoints = Math.max(3, maxCodePoints);
    }

    /**
     * 将双语内容渲染为短信模板变量。
     *
     * @param content 双语内容
     * @return 不超过配置 code point 数量的双语正文
     */
    public String render(BilingualContent content) {
        if (content == null) {
            return "";
        }
        String chinese = content.zhCn();
        String uyghur = content.ugCn();
        int chineseLength = codePointLength(chinese);
        int uyghurLength = codePointLength(uyghur);
        int available = maxCodePoints - 1;
        int chineseBudget = available / 2;
        int uyghurBudget = available - chineseBudget;

        if (chineseLength < chineseBudget) {
            chineseBudget = chineseLength;
            uyghurBudget = available - chineseBudget;
        } else if (uyghurLength < uyghurBudget) {
            uyghurBudget = uyghurLength;
            chineseBudget = available - uyghurBudget;
        }
        return truncate(chinese, chineseBudget) + "\n" + truncate(uyghur, uyghurBudget);
    }

    private static int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private static String truncate(String value, int maxLength) {
        if (maxLength <= 0 || value.isEmpty()) {
            return "";
        }
        int length = codePointLength(value);
        if (length <= maxLength) {
            return value;
        }
        return value.substring(0, value.offsetByCodePoints(0, maxLength));
    }
}
