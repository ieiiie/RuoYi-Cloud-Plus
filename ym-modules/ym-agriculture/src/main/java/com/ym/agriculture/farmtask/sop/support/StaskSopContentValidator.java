package com.ym.agriculture.farmtask.sop.support;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopContentBlockVo;
import com.ym.resource.api.domain.RemoteFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SOP 富媒体内容块白名单清洗与结构校验。 */
@Component
@lombok.RequiredArgsConstructor
public class StaskSopContentValidator {

    private static final Pattern VIDEO_ELEMENT_PATTERN = Pattern.compile("(?is)<video\\b[^>]*(?:/>|>.*?</video>)");

    private final StaskSopMasterOssAccessor ossAccessor;

    /** 将请求内容块清洗为可持久化 JSON。 */
    public String sanitize(List<Map<String, Object>> blocks) {
        if (CollUtil.isEmpty(blocks)) {
            throw new ServiceException("SOP内容不能为空");
        }
        List<SfStaskSopContentBlockVo> normalized = new ArrayList<>();
        for (Map<String, Object> block : blocks) {
            SfStaskSopContentBlockVo content = JSON.parseObject(JSON.toJSONString(block), SfStaskSopContentBlockVo.class);
            normalized.add(normalize(content));
        }
        if (normalized.isEmpty()) {
            throw new ServiceException("SOP内容不能为空");
        }
        return JSON.toJSONString(normalized);
    }

    /** 解析持久化 JSON，返回保持原有顺序的内容块。 */
    public List<SfStaskSopContentBlockVo> parse(String contentBlocks) {
        if (StrUtil.isBlank(contentBlocks)) {
            return List.of();
        }
        JSONArray array = JSON.parseArray(contentBlocks);
        List<SfStaskSopContentBlockVo> result = new ArrayList<>();
        for (Object item : array) {
            result.add(JSON.parseObject(JSON.toJSONString(item), SfStaskSopContentBlockVo.class));
        }
        return result;
    }

    /**
     * 将后台单一富文本正文转换为小程序顺序内容块。
     * 历史独立 VIDEO 块原样保留，便于旧数据兼容。
     */
    public List<SfStaskSopContentBlockVo> parseForMiniapp(String contentBlocks) {
        List<SfStaskSopContentBlockVo> result = new ArrayList<>();
        for (SfStaskSopContentBlockVo block : parse(contentBlocks)) {
            if ("RICH_TEXT".equals(block.getType())) {
                result.addAll(splitRichTextVideos(block.getHtml()));
            } else {
                result.add(block);
            }
        }
        return result;
    }

    private SfStaskSopContentBlockVo normalize(SfStaskSopContentBlockVo block) {
        if (block == null || StrUtil.isBlank(block.getType())) {
            throw new ServiceException("SOP内容块类型不能为空");
        }
        if ("RICH_TEXT".equals(block.getType())) {
            String html = Jsoup.clean(StrUtil.blankToDefault(block.getHtml(), ""), safelist());
            if (StrUtil.isBlank(Jsoup.parse(html).text()) && !html.contains("<img") && !html.contains("<video")) {
                throw new ServiceException("富文本内容不能为空");
            }
            validateRichTextMedia(html);
            block.setHtml(html);
            block.setAssetId(null);
            block.setUrl(null);
            block.setCoverUrl(null);
            block.setTitle(null);
            return block;
        }
        if ("VIDEO".equals(block.getType())) {
            if (StrUtil.isBlank(block.getAssetId()) || !isHttps(block.getUrl())) {
                throw new ServiceException("视频必须提供OSS文件和HTTPS播放地址");
            }
            if (StrUtil.isNotBlank(block.getCoverUrl()) && !isHttps(block.getCoverUrl())) {
                throw new ServiceException("视频封面必须使用HTTPS地址");
            }
            validateOssReference(block.getAssetId(), block.getUrl(), "视频");
            block.setHtml(null);
            return block;
        }
        throw new ServiceException("不支持的SOP内容块类型");
    }

    private static Safelist safelist() {
        return Safelist.relaxed()
            .addTags("video", "source")
            .removeTags("iframe", "style", "script")
            .removeAttributes(":all", "style", "onload", "onclick", "onerror")
            .addAttributes("img", "data-oss-id")
            .addAttributes("video", "src", "controls", "preload", "width", "height", "title", "poster", "data-oss-id", "data-cover-oss-id")
            .addAttributes("source", "src", "type", "data-oss-id");
    }

    private static boolean isHttps(String value) {
        return StrUtil.startWithIgnoreCase(StrUtil.trim(value), "https://");
    }

    private void validateRichTextMedia(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        for (Element image : document.select("img")) {
            validateEmbeddedOss(image, "富文本图片");
        }
        for (Element video : document.select("video")) {
            if (StrUtil.isNotBlank(video.attr("poster"))) {
                validateOssReference(video.attr("data-cover-oss-id"), video.attr("poster"), "富文本视频封面");
            }
            if (StrUtil.isNotBlank(video.attr("src"))) {
                validateEmbeddedOss(video, "富文本视频");
                continue;
            }
            List<Element> sources = video.children().stream()
                .filter(element -> "source".equals(element.tagName()))
                .toList();
            if (CollUtil.isEmpty(sources)) {
                throw new ServiceException("富文本视频必须提供已上传的OSS文件");
            }
            for (Element source : sources) {
                String assetId = StrUtil.blankToDefault(source.attr("data-oss-id"), video.attr("data-oss-id"));
                validateOssReference(assetId, source.attr("src"), "富文本视频");
            }
        }
    }

    private void validateEmbeddedOss(Element element, String mediaName) {
        validateOssReference(element.attr("data-oss-id"), element.attr("src"), mediaName);
    }

    private List<SfStaskSopContentBlockVo> splitRichTextVideos(String html) {
        if (StrUtil.isBlank(html)) {
            return List.of();
        }
        List<SfStaskSopContentBlockVo> result = new ArrayList<>();
        Matcher matcher = VIDEO_ELEMENT_PATTERN.matcher(html);
        int offset = 0;
        while (matcher.find()) {
            addRichTextIfPresent(result, html.substring(offset, matcher.start()));
            result.add(toMiniappVideoBlock(matcher.group()));
            offset = matcher.end();
        }
        addRichTextIfPresent(result, html.substring(offset));
        return result;
    }

    private void addRichTextIfPresent(List<SfStaskSopContentBlockVo> result, String html) {
        Document document = Jsoup.parseBodyFragment(StrUtil.blankToDefault(html, ""));
        if (StrUtil.isBlank(document.text()) && document.select("img").isEmpty()) {
            return;
        }
        SfStaskSopContentBlockVo richText = new SfStaskSopContentBlockVo();
        richText.setType("RICH_TEXT");
        richText.setHtml(html);
        result.add(richText);
    }

    private SfStaskSopContentBlockVo toMiniappVideoBlock(String videoHtml) {
        Element video = Jsoup.parseBodyFragment(videoHtml).selectFirst("video");
        if (video == null) {
            throw new ServiceException("SOP视频内容解析失败");
        }
        Element source = video.selectFirst("source");
        String assetId = StrUtil.isNotBlank(video.attr("data-oss-id"))
            ? video.attr("data-oss-id")
            : source == null ? null : source.attr("data-oss-id");
        String url = StrUtil.isNotBlank(video.attr("src"))
            ? video.attr("src")
            : source == null ? null : source.attr("src");
        SfStaskSopContentBlockVo result = new SfStaskSopContentBlockVo();
        result.setType("VIDEO");
        result.setAssetId(assetId);
        result.setUrl(url);
        result.setCoverUrl(StrUtil.isBlank(video.attr("poster")) ? null : video.attr("poster"));
        result.setTitle(StrUtil.isBlank(video.attr("title")) ? null : video.attr("title"));
        return result;
    }

    private void validateOssReference(String assetId, String url, String mediaName) {
        if (StrUtil.isBlank(assetId) || !isHttps(url)) {
            throw new ServiceException(mediaName + "必须引用已上传的HTTPS OSS文件");
        }
        try {
            RemoteFile oss = ossAccessor.getById(Long.valueOf(assetId));
            if (oss == null || !StrUtil.equals(StrUtil.trim(url), StrUtil.trim(oss.getUrl()))) {
                throw new ServiceException(mediaName + "必须引用当前可用的OSS文件");
            }
        } catch (NumberFormatException exception) {
            throw new ServiceException(mediaName + "OSS文件标识不合法");
        }
    }
}
