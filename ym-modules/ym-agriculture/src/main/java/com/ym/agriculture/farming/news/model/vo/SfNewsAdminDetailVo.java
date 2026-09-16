package com.ym.agriculture.farming.news.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/** 管理端农业资讯审稿详情。 */
@Data
public class SfNewsAdminDetailVo {

    /** 文章主键。 */
    private Long articleId;
    /** 当前版本主键。 */
    private Long revisionId;
    /** 当前版本号。 */
    private Integer revisionNo;
    /** 线上版本主键。 */
    private Long publishedRevisionId;
    /** 标题。 */
    private String title;
    /** 摘要。 */
    private String summary;
    /** 分类。 */
    private String category;
    /** 来源名称。 */
    private String sourceName;
    /** 作者。 */
    private String author;
    /** 原文发布时间。 */
    private Date originPublishedAt;
    /** 原文地址。 */
    private String originUrl;
    /** 封面地址。 */
    private String coverUrl;
    /** 标签。 */
    private List<String> tags;
    /** 清洗后、可编辑的 HTML。 */
    private String sanitizedHtml;
    /** 移动端内容块。 */
    private List<Object> contentBlocks;
    /** 文章状态。 */
    private String articleStatus;
    /** 审核状态。 */
    private String reviewStatus;
    /** 审核人名称。 */
    private String reviewerName;
    /** 审核意见。 */
    private String reviewComment;
    /** 审核时间。 */
    private Date reviewedAt;
    /** 是否推荐。 */
    private Boolean recommendFlag;
    /** 推荐排序。 */
    private Integer sortOrder;
    private String mediaStatus;
    private Integer mediaTotal;
    private Integer mediaSucceeded;
    private Integer mediaFailed;
    private List<SfNewsMediaItemVo> mediaItems;
}
