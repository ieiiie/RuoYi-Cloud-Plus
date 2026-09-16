package com.ym.agriculture.farming.integration.ai.algback.dto.imageinfer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片推理提交请求 — 对应 {@code POST /imageInfer/submitTask}。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AlgBackImageInferSubmitRequest {

    /** 算法模型编号 */
    private String modelNo;

    /** 单张图片 URL（与 imageUrls 二选一） */
    private String imageUrl;

    /** 多张图片 URL 列表（与 imageUrl 二选一） */
    private List<String> imageUrls;

    /** 回调通知地址 */
    private String callbackUrl;

    /** 自定义业务参数，回调时原样透传 */
    private Object bizParams;
}
