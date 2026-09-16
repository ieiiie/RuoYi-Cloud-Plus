package com.ym.agriculture.farming.news.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("sf_news_media_transfer")
public class SfNewsMediaTransfer implements Serializable {
    @Serial private static final long serialVersionUID = 1L;
    @TableId("task_id") private Long taskId;
    private Long revisionId;
    private String mediaType;
    private String sourceUrl;
    private String sourceUrlSha256;
    private String transferStatus;
    private Integer attemptCount;
    private Date nextRetryAt;
    private Long ossId;
    private String ossUrl;
    private String contentType;
    private Long fileSize;
    private String errorCode;
    private String errorMessage;
    private Date processingStartedAt;
    private Date completedAt;
    private Date createTime;
    private Date updateTime;
}
