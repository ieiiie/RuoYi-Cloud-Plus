package com.ym.agriculture.farming.news.model.constants;

/** 农业资讯状态与分类常量。 */
public interface SfNewsConstants {

    String ARTICLE_REVIEWING = "REVIEWING";
    String ARTICLE_REJECTED = "REJECTED";
    String ARTICLE_PUBLISHED = "PUBLISHED";
    String ARTICLE_OFFLINE = "OFFLINE";

    String REVIEW_PENDING = "PENDING";
    String REVIEW_APPROVED = "APPROVED";
    String REVIEW_REJECTED = "REJECTED";

    String INGEST_PENDING = "PENDING";
    String INGEST_PROCESSING = "PROCESSING";
    String INGEST_ACCEPTED = "ACCEPTED";
    String INGEST_UNCHANGED = "UNCHANGED";
    String INGEST_REJECTED = "REJECTED";
    String INGEST_FAILED = "FAILED";

    String MEDIA_NONE = "NONE";
    String MEDIA_TRANSFERRING = "TRANSFERRING";
    String MEDIA_READY = "READY";
    String MEDIA_FAILED = "FAILED";

    String CATEGORY_POLICY = "policy";
    String CATEGORY_KNOWLEDGE = "knowledge";
    String CATEGORY_MARKET = "market";
    String CATEGORY_GENERAL = "general";
}
