package com.ym.agriculture.farming.news.model.vo;

import lombok.Data;

@Data
public class SfNewsMediaItemVo {
    private Long taskId;
    private String mediaType;
    private String sourceUrl;
    private String transferStatus;
    private Long ossId;
    private String ossUrl;
    private String contentType;
    private Long fileSize;
    private Integer attemptCount;
    private String errorCode;
    private String errorMessage;
}
