package com.ym.agriculture.farming.news.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/** 移动端农业资讯详情。 */
@Data
public class SfNewsMobileDetailVo {

    /** 文章主键。 */
    private Long articleId;
    /** 标题。 */
    private String title;
    /** 摘要。 */
    private String summary;
    /** 分类。 */
    private String category;
    /** 分类名称。 */
    private String categoryName;
    /** 来源名称。 */
    private String source;
    /** 作者。 */
    private String author;
    /** 平台发布时间。 */
    private Date publishTime;
    /** 原文地址。 */
    private String originUrl;
    /** 封面地址。 */
    private String coverUrl;
    /** 后台预览 HTML；移动端不应作为主渲染数据。 */
    private String sanitizedHtml;
    /** 移动端结构化内容块。 */
    private List<Object> contentBlocks;
    /** 标签。 */
    private List<String> tags;
}
