package com.ym.agriculture.farming.algback.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 提交算法任务（当前租户须已配置中台 {@code customer_no}）。
 * <p>
 * 用于向算法中台发起一次视频分析任务申请，指定使用的模型、视频源以及推理相关参数。
 *
 * @author ym-cloud
 */
@Data
public class AlgBackTaskSubmitBo {

    /**
     * 算法模型编号，在算法中台/AI 平台中配置的模型标识，
     * 用于指定本次任务使用哪一个算法模型进行推理。
     */
    @NotBlank
    private String modelNo;

    /**
     * 待分析视频播放地址，例如 HTTP/RTSP/RTMP 等可访问的 URL，
     * 算法侧通过该地址拉取视频流或读取视频文件。
     */
    @NotBlank
    private String videoPlayUrl;

    /**
     * 算法任务名称，用于在任务列表中标识本次任务，例如“仓库通道-人流检测”。
     */
    private String taskName;

    /**
     * 跳帧间隔配置，例如设为 5 表示每 5 帧取一帧进行推理，
     * 用于在可接受精度范围内降低计算开销。
     */
    private Integer skipFrame;

    /**
     * 结果推送频率，控制算法中台向本系统回传检测结果的频率
     * （具体单位依中台接口定义，通常为秒或帧数）。
     */
    private Integer pushFrequency;

    /**
     * 置信度阈值（confidence threshold），低于该置信度的检测结果将被过滤掉，
     * 一般取值范围为 0~1，如 0.5 表示只保留置信度大于等于 0.5 的目标。
     */
    private Float confThreshold;

    /**
     * NMS 阈值（non-maximum suppression threshold），用于去除高度重叠的检测框，
     * 一般取值 0.3~0.6，重叠度超过该阈值的框会被抑制。
     */
    private Float nmsThreshold;

    /**
     * 视频基础信息补充字段，通常为 JSON 字符串，
     * 可包含分辨率、帧率、摄像头位置、业务场景等元数据。
     */
    private String videoBaseInfo;

    /**
     * 为 true 时创建成功后自动调用中台启动（{@code taskStatus=1}）
     */
    private boolean startAfterCreate = true;
}
