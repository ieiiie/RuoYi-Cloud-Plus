package com.ym.agriculture.farming.news.model.vo;

import lombok.Data;

import java.util.Date;

/** 管理端农业资讯审核列表项。 */
@Data
public class SfNewsAdminListVo {

    /** 文章主键。 */
    private Long articleId;
    /** 当前版本主键。 */
    private Long revisionId;
    /** 当前版本号。 */
    private Integer revisionNo;
    /** 标题。 */
    private String title;
    /** 摘要。 */
    private String summary;
    /** 分类。 */
    private String category;
    /** 来源名称。 */
    private String sourceName;
    /** 文章状态。 */
    private String articleStatus;
    /** 当前版本审核状态。 */
    private String reviewStatus;
    /** 是否推荐。 */
    private Boolean recommendFlag;
    /** 原文发布时间。 */
    private Date originPublishedAt;
    /** 平台发布时间。 */
    private Date publishTime;
    /** 当前版本创建时间。 */
    private Date createTime;
    private String mediaStatus;
    private Integer mediaTotal;
    private Integer mediaSucceeded;
    private Integer mediaFailed;
}
