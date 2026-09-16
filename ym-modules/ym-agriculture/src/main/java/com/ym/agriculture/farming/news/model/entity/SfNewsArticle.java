package com.ym.agriculture.farming.news.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/** 平台全局农业资讯文章身份。 */
@Data
@TableName("sf_news_article")
public class SfNewsArticle implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 文章主键。 */
    @TableId("article_id")
    private Long articleId;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源内稳定文章标识。 */
    private String externalArticleKey;
    /** 规范化原文地址。 */
    private String canonicalUrl;
    /** 最近一次原文地址。 */
    private String originUrl;
    /** 最近一次来源正文 SHA-256，不随审核人员编辑而改变。 */
    private String latestSourceSha256;
    /** 当前待处理版本主键。 */
    private Long currentRevisionId;
    /** 当前线上版本主键。 */
    private Long publishedRevisionId;
    /** 文章状态：REVIEWING/REJECTED/PUBLISHED/OFFLINE。 */
    private String articleStatus;
    /** 是否推荐。 */
    private Boolean recommendFlag;
    /** 推荐排序，升序。 */
    private Integer sortOrder;
    /** 阅读次数。 */
    private Long readCount;
    /** 首次或最近发布时刻。 */
    private Date publishTime;
    /** 删除标志：0-存在，1-删除。 */
    @TableLogic
    private String delFlag;
    /** 创建人。 */
    private Long createBy;
    /** 创建时间。 */
    private Date createTime;
    /** 更新人。 */
    private Long updateBy;
    /** 更新时间。 */
    private Date updateTime;
}
