package com.ym.agriculture.farming.news.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 农业资讯版本；审核员可在已通过状态下直接更正当前版本。 */
@Data
@TableName("sf_news_revision")
public class SfNewsRevision implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 版本主键。 */
    @TableId("revision_id")
    private Long revisionId;
    /** 文章主键。 */
    private Long articleId;
    /** 文章内递增版本号。 */
    private Integer revisionNo;
    /** 来源暂存请求 ID。 */
    private String sourceRequestId;
    /** 标题。 */
    private String title;
    /** 摘要。 */
    private String summary;
    /** 分类：policy/knowledge/market/general。 */
    private String category;
    /** 展示来源。 */
    private String sourceName;
    /** 作者。 */
    private String author;
    /** 原文发布时间。 */
    private Date originPublishedAt;
    /** 原文地址。 */
    private String originUrl;
    /** 封面 OSS 地址。 */
    private String coverUrl;
    /** 标签 JSON 数组。 */
    private String tagsJson;
    /** 白名单清洗后的 HTML，仅供后台审稿。 */
    private String sanitizedHtml;
    /** 移动端结构化正文 JSON 数组。 */
    private String contentBlocksJson;
    /** 当前版本正文 SHA-256。 */
    private String contentSha256;
    private String mediaStatus;
    private Integer mediaTotal;
    private Integer mediaSucceeded;
    private Integer mediaFailed;
    /** 审核状态：PENDING/APPROVED/REJECTED。 */
    private String reviewStatus;
    /** 审核人 ID。 */
    private Long reviewerId;
    /** 审核人名称快照。 */
    private String reviewerName;
    /** 审核意见。 */
    private String reviewComment;
    /** 审核时间。 */
    private Date reviewedAt;
    /** 创建人。 */
    private Long createBy;
    /** 创建时间。 */
    private Date createTime;
    /** 更新人。 */
    private Long updateBy;
    /** 更新时间。 */
    private Date updateTime;
}
