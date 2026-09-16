package com.ym.agriculture.farming.algback.model.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 算法中台 HTTP 推送体字段子集
 *
 * @author ym-cloud
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackInferencePushDto {

    private String taskNo;
    private String taskName;
    private String modelNo;
    private String modelName;
    private String customerNo;
    private String customerName;
    private Long algorithmTypeId;
    private String algorithmTypeValue;

    /**
     * 分类得分信息。部分中台实现为字符串形式的 JSON / KV（例如 {@code {"Car":0.742}}），
     * 因此使用 {@code String} 进行接收，后续由业务自行解析。
     */
    private String clsScore;

    private String imgUrl;

    /**
     * 告警时间。
     * <p>
     * 中台可能传入毫秒时间戳或格式化时间字符串（例如 {@code 2026-04-01 14:02:11}），
     * 为避免反序列化错误，这里统一按 {@code String} 接收，后续在业务层进行解析与转换。
     */
    @JsonAlias({"alarm_time"})
    private String alarmTime;
    private String videoPlayUrl;
    private String streamServerUrl;
    private String computingVideoPlayUrl;
    private String pushVideoPlayUrl;
}
