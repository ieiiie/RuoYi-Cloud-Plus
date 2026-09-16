package com.ym.agriculture.farming.weatheralert.support;

import com.ym.common.core.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * 预警详情 HTML 尽力解析；失败时返回空字段，不抛错。
 */
public final class NmcAlertDetailHtmlParser {

    private NmcAlertDetailHtmlParser() {
    }

    public static ParsedDetail parse(String html) {
        if (StringUtils.isBlank(html)) {
            return ParsedDetail.empty();
        }
        try {
            Document doc = Jsoup.parse(html);
            String description = firstText(doc, "#alarmtext", ".alarmtext", "#text", ".writing");
            if (StringUtils.isBlank(description)) {
                Element main = doc.selectFirst("#alarmdiv, .alarm, #content, .content");
                if (main != null) {
                    description = main.text();
                }
            }
            List<String> instructions = new ArrayList<>();
            Elements guides = doc.select("#fygj li, .fygj li, #guide li, .defense li, ol li");
            for (Element li : guides) {
                String t = li.text();
                if (StringUtils.isNotBlank(t)) {
                    instructions.add(t.trim());
                }
            }
            if (instructions.isEmpty() && StringUtils.isNotBlank(description)) {
                // 无独立防御指南时不强造
            }
            String publisher = firstText(doc, "#publisher", ".publisher", "#author");
            return new ParsedDetail(
                StringUtils.isBlank(description) ? null : description.trim(),
                instructions,
                StringUtils.isBlank(publisher) ? null : publisher.trim()
            );
        } catch (Exception ex) {
            return ParsedDetail.empty();
        }
    }

    private static String firstText(Document doc, String... cssQueries) {
        for (String q : cssQueries) {
            Element el = doc.selectFirst(q);
            if (el != null && StringUtils.isNotBlank(el.text())) {
                return el.text();
            }
        }
        return null;
    }

    public record ParsedDetail(String description, List<String> instructions, String publisher) {
        public static ParsedDetail empty() {
            return new ParsedDetail(null, List.of(), null);
        }
    }
}
