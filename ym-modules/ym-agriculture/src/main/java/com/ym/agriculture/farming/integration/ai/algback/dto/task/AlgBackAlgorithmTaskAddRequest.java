package com.ym.agriculture.farming.integration.ai.algback.dto.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建计算任务 {@code POST /algorithmTask/addAlgorithmTask}。
 * <p>
 * 新建默认 {@code taskStatus=0}，需 {@code setAlgorithmTaskStatus} 启动；{@code videoPlayUrl} 须满足中台校验。
 *
 * @author ym-cloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlgBackAlgorithmTaskAddRequest {

    /** 须已存在；与 {@link #customerNo} 一起建任务。 */
    private String modelNo;

    private String customerNo;

    /** 原始视频流地址。 */
    private String videoPlayUrl;

    /** 可选；默认取模型名。 */
    private String taskName;

    /** 跳帧；默认 1。 */
    private Integer skipFrame;

    /** 推送频率（秒）；默认 60。 */
    private Integer pushFrequency;

    /** 置信度阈值；默认取模型。 */
    private Float confThreshold;

    /** NMS 阈值；默认取模型。 */
    private Float nmsThreshold;

    /** 视频元信息 JSON。 */
    private String videoBaseInfo;

    /** 仅更新任务（ValidationUpdate）时必填；新增不要传。 */
    private Long id;
}
