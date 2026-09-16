package com.ym.agriculture.farming.news.support;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.parser.Tag;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 将外部正文收敛为可审稿 HTML 和受控 contentBlocks。
 *
 * <p>不保留脚本、事件、iframe、任意 class/style；审核通过前还会校验所有媒体均已转存 OSS。</p>
 */
@Component
public class SfNewsContentSupport {

    private static final Safelist CONTENT_SAFELIST = new Safelist()
        .addTags("p", "h1", "h2", "h3", "strong", "b", "em", "i", "u", "span", "br",
            "blockquote", "ul", "ol", "li", "img", "video", "source")
        .addAttributes("p", "align")
        .addAttributes("img", "src", "alt", "title", "data-oss-id")
        .addAttributes("video", "src", "poster", "title", "controls", "data-oss-id")
        .addAttributes("video", "data-poster-oss-id")
        .addAttributes("source", "src", "type", "data-oss-id")
        .addProtocols("img", "src", "http", "https")
        .addProtocols("video", "src", "http", "https")
        .addProtocols("video", "poster", "http", "https")
        .addProtocols("source", "src", "http", "https");

    /**
     * 清洗正文并生成移动端内容块。
     *
     * @param html 爬虫抽取或审核人员编辑后的 HTML
     * @return 清洗与结构化结果
     */
    public SfNewsContentResult sanitizeAndConvert(String html) {
        return sanitizeAndConvert(html, "");
    }

    /** 清洗正文前先依据原文地址补全相对媒体地址。 */
    public SfNewsContentResult sanitizeAndConvert(String html, String baseUri) {
        Document source = Jsoup.parseBodyFragment(html == null ? "" : html, baseUri == null ? "" : baseUri);
        source.select("img[src],video[src],video source[src]").forEach(element -> {
            String absolute = element.absUrl("src");
            if (!absolute.isBlank()) element.attr("src", absolute);
        });
        source.select("video[poster]").forEach(element -> {
            String absolute = element.absUrl("poster");
            if (!absolute.isBlank()) element.attr("poster", absolute);
        });
        Document.OutputSettings settings = new Document.OutputSettings().prettyPrint(false);
        String cleaned = Jsoup.clean(source.body().html(), "", CONTENT_SAFELIST, settings);
        Document document = Jsoup.parseBodyFragment(cleaned);
        normalizeTopLevelTextNodes(document.body());
        String sanitized = document.body().html();
        JSONArray blocks = new JSONArray();
        for (Element element : document.body().children()) {
            appendBlock(element, blocks);
        }
        if (blocks.isEmpty()) {
            throw new ServiceException("正文清洗后为空，请检查采集内容或重新编辑");
        }
        Element firstImage = document.selectFirst("img[src]");
        return new SfNewsContentResult(sanitized, blocks.toJSONString(),
            firstImage == null ? null : firstImage.attr("src"));
    }

    /**
     * 审核通过前校验媒体已通过后台上传到 OSS。
     *
     * @param sanitizedHtml 清洗后的正文
     */
    public void validatePublishableMedia(String sanitizedHtml) {
        Document document = Jsoup.parseBodyFragment(sanitizedHtml == null ? "" : sanitizedHtml);
        for (Element image : document.select("img[src]")) {
            if (image.attr("data-oss-id").isBlank()) {
                throw new ServiceException("正文存在未转存到平台 OSS 的图片，请删除或在编辑器中重新上传");
            }
        }
        for (Element video : document.select("video[src], video source[src]")) {
            if (video.attr("data-oss-id").isBlank() && video.parent().attr("data-oss-id").isBlank()) {
                throw new ServiceException("正文存在未转存到平台 OSS 的视频，请删除或转存后再审核通过");
            }
        }
        for (Element video : document.select("video[poster]")) {
            if (video.attr("data-poster-oss-id").isBlank()) {
                throw new ServiceException("视频封面尚未转存到平台 OSS，请删除封面或等待自动转存完成");
            }
        }
    }

    /** 将编辑器直接提交的顶层文本规范化为段落，确保正文能生成移动端内容块。 */
    private static void normalizeTopLevelTextNodes(Element body) {
        List<Node> nodes = new ArrayList<>(body.childNodes());
        for (Node node : nodes) {
            if (!(node instanceof TextNode textNode)) {
                continue;
            }
            if (textNode.getWholeText().isBlank()) {
                textNode.remove();
                continue;
            }
            Element paragraph = new Element(Tag.valueOf("p"), body.baseUri());
            paragraph.text(textNode.getWholeText());
            textNode.replaceWith(paragraph);
        }
    }

    private void appendBlock(Element element, JSONArray blocks) {
        String tag = element.normalName();
        switch (tag) {
            case "p" -> appendParagraph(element, blocks);
            case "h1", "h2", "h3" -> appendHeading(element, blocks);
            case "blockquote" -> appendQuote(element, blocks);
            case "ul", "ol" -> appendList(element, blocks);
            case "img" -> appendImage(element, blocks);
            case "video" -> appendVideo(element, blocks);
            default -> element.children().forEach(child -> appendBlock(child, blocks));
        }
    }

    private void appendParagraph(Element element, JSONArray blocks) {
        if (element.text().isBlank() && element.select("img,video").isEmpty()) {
            return;
        }
        if (!element.text().isBlank()) {
            JSONObject block = new JSONObject();
            block.put("type", "paragraph");
            block.put("text", element.text());
            String align = element.attr("align");
            if (Set.of("left", "center", "right").contains(align)) {
                block.put("align", align);
            }
            JSONArray runs = new JSONArray();
            appendRuns(element, new LinkedHashSet<>(), runs);
            if (!runs.isEmpty()) {
                block.put("runs", runs);
            }
            blocks.add(block);
        }
        element.children().stream()
            .filter(child -> Set.of("img", "video").contains(child.normalName()))
            .forEach(child -> appendBlock(child, blocks));
    }

    private void appendRuns(Node node, Set<String> inheritedMarks, JSONArray runs) {
        if (node instanceof TextNode textNode) {
            if (!textNode.getWholeText().isEmpty()) {
                JSONObject run = new JSONObject();
                run.put("text", textNode.getWholeText());
                if (!inheritedMarks.isEmpty()) {
                    run.put("marks", new ArrayList<>(inheritedMarks));
                }
                runs.add(run);
            }
            return;
        }
        Set<String> marks = new LinkedHashSet<>(inheritedMarks);
        if (node instanceof Element element) {
            switch (element.normalName()) {
                case "strong", "b" -> marks.add("bold");
                case "em", "i" -> marks.add("italic");
                case "u" -> marks.add("underline");
                default -> { }
            }
        }
        node.childNodes().forEach(child -> appendRuns(child, marks, runs));
    }

    private void appendHeading(Element element, JSONArray blocks) {
        if (element.text().isBlank()) {
            return;
        }
        JSONObject block = new JSONObject();
        block.put("type", "heading");
        block.put("text", element.text());
        block.put("level", "h3".equals(element.normalName()) ? 3 : 2);
        blocks.add(block);
    }

    private void appendQuote(Element element, JSONArray blocks) {
        if (element.text().isBlank()) {
            return;
        }
        JSONObject block = new JSONObject();
        block.put("type", "quote");
        block.put("text", element.text());
        blocks.add(block);
    }

    private void appendList(Element element, JSONArray blocks) {
        List<String> items = element.children().stream()
            .filter(child -> "li".equals(child.normalName()))
            .map(Element::text)
            .filter(text -> !text.isBlank())
            .toList();
        if (items.isEmpty()) {
            return;
        }
        JSONObject block = new JSONObject();
        block.put("type", "list");
        block.put("ordered", "ol".equals(element.normalName()));
        block.put("items", items);
        blocks.add(block);
    }

    private void appendImage(Element element, JSONArray blocks) {
        if (element.attr("src").isBlank()) {
            return;
        }
        JSONObject block = new JSONObject();
        block.put("type", "image");
        block.put("url", element.attr("src"));
        block.put("alt", element.attr("alt"));
        block.put("caption", element.attr("title"));
        blocks.add(block);
    }

    private void appendVideo(Element element, JSONArray blocks) {
        String url = element.attr("src");
        if (url.isBlank()) {
            Element source = element.selectFirst("source[src]");
            url = source == null ? "" : source.attr("src");
        }
        if (url.isBlank()) {
            return;
        }
        JSONObject block = new JSONObject();
        block.put("type", "video");
        block.put("url", url);
        block.put("poster", element.attr("poster"));
        block.put("title", element.attr("title"));
        blocks.add(block);
    }
}
