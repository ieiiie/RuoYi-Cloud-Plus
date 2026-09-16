package com.ym.agriculture.farming.news.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/** 爬虫直写的农业资讯采集暂存记录。 */
@Data
@TableName("sf_news_ingest_staging")
public class SfNewsIngestStaging {

    /** 暂存主键。 */
    @TableId("id")
    private Long id;
    /** 爬虫逻辑提交唯一 ID。 */
    private String requestId;
    /** 采集批次。 */
    private String crawlRunId;
    /** 来源编码。 */
    private String sourceCode;
    /** 来源内稳定文章标识。 */
    private String externalArticleKey;
    /** 原网站文章 ID。 */
    private String sourceArticleId;
    /** 原始地址。 */
    private String originUrl;
    /** 规范地址。 */
    private String canonicalUrl;
    /** UTC 采集时间。 */
    private Date fetchedAtUtc;
    /** 载荷协议版本。 */
    private String payloadSchemaVersion;
    /** 元数据 JSON。 */
    private String payloadJson;
    /** 抽取后的正文 HTML。 */
    private String extractedHtml;
    /** 原始页面 HTML。 */
    private String rawHtml;
    /** 正文 SHA-256。 */
    private String contentSha256;
    /** 原始页面 SHA-256。 */
    private String rawSha256;
    /** 完整载荷 SHA-256。 */
    private String payloadSha256;
    /** 平台处理状态。 */
    private String ingestStatus;
    /** 处理动作。 */
    private String resultAction;
    /** 处理次数。 */
    private Integer processAttempts;
    /** 告警 JSON。 */
    private String warningsJson;
    /** 错误码。 */
    private String errorCode;
    /** 错误说明。 */
    private String errorMessage;
    /** 文章主键。 */
    private Long articleId;
    /** 版本主键。 */
    private Long revisionId;
    /** 接收时间。 */
    private Date receivedAt;
    /** 处理开始时间。 */
    private Date processingStartedAt;
    /** 处理完成时间。 */
    private Date processedAt;
}
