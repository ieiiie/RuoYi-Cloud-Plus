package com.ym.agriculture.farming.satellite.controller;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ym.common.core.domain.R;
import com.ym.agriculture.farming.satellite.model.dto.SatelliteCallbackDto;
import com.ym.agriculture.farming.satellite.service.impl.SfSatelliteCallbackServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 卫星遥感结果回调接口。
 * <p>
 * 对应《遥感数据接收接口说明》，用于接收外部遥感服务按任务推送的计算结果，
 * 请求体为形如 {@code {"data": {dk_id, dk_bounds, task_type, area, object_key1, image_pixel, image_date, bucket, ...}}} 的 JSON：
 * <ul>
 *     <li>{@code dk_id} 地块标识，与任务创建时返回的一致</li>
 *     <li>{@code dk_bounds} 影像边界和中心点（WGS84 坐标）</li>
 *     <li>{@code task_type} 服务类型，如 {@code growth}、{@code soilmoisture} 等</li>
 *     <li>{@code area} 各等级面积数组（5 个等级），含义与 {@code task_type} 对应的等级字典一致</li>
 *     <li>{@code object_key1} 差异影像数据 OSS 对象键</li>
 *     <li>{@code image_pixel} 影像空间分辨率</li>
 *     <li>{@code image_date} 影像拍摄时间</li>
 *     <li>{@code bucket} 存储桶名称，部分场景用于承载错误信息</li>
 * </ul>
 * 平台收到回调后，会将结果持久化到 {@code sf_satellite_task_result} 并更新任务主表状态。
 * <p>
 * 注意：若接入 Sa-Token 需将本路径加入匿名白名单，保证公网遥感服务可直接访问。
 */
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/satellite")
public class SfSatelliteCallbackController {

    private final SfSatelliteCallbackServiceImpl callbackService;
    private final ObjectMapper objectMapper;

    /**
     * 接收遥感结果回调。
     * <p>
     * 外部遥感服务调用 {@code /smart-farming/satellite/callback/result}，传入包含 {@code data} 节点的 JSON 请求体，
     * 从 {@code data} 中解析回调业务字段并转换为 {@link SatelliteCallbackDto}，随后委托
     * {@link SfSatelliteCallbackServiceImpl#processCallback(SatelliteCallbackDto)} 进行持久化和任务状态更新。
     *
     * @param body 原始请求体 Map，一般为 {@code {"data": {...}}} 结构；如果上游直接传平铺字段，也能兼容解析
     * @return {@code R<Void>} 通用响应包装，响应体字段说明：
     * <ul>
     *     <li>{@code code} 业务状态码，200 表示平台处理成功，其他表示失败</li>
     *     <li>{@code msg} 提示信息，失败时包含具体错误原因（如解析异常、任务不存在等）</li>
     *     <li>{@code data} 此回调接口为 {@code null}</li>
     * </ul>
     * 具体返回形态示例：
     * <pre>
     * {"code":200,"msg":"success","data":null}
     * {"code":500,"msg":"回调处理失败: xxx","data":null}
     * </pre>
     * 外部服务通常只关心 HTTP 是否为 200，但通过 {@code R} 包装可以方便平台统一记录日志和调试。
     */
    @PostMapping("/callback/result")
    public R<Void> receiveCallback(@RequestBody Map<String, Object> body) {
        try {
            // 文档约定：{"data": {dk_id, dk_bounds, ...}}，此处从 data 节点中解析业务字段
            Object dataNode = body.getOrDefault("data", body);
            log.info("卫星回调原始数据：{}", JSON.toJSONString(body));
            SatelliteCallbackDto dto = objectMapper.convertValue(dataNode, SatelliteCallbackDto.class);
            log.info("收到遥感回调 dkId={}", dto != null ? dto.getDkId() : null);
            callbackService.processCallback(dto);
            return R.ok();
        } catch (Exception e) {
            log.error("遥感回调处理异常", e);
            return R.fail("回调处理失败: " + e.getMessage());
        }
    }
}
