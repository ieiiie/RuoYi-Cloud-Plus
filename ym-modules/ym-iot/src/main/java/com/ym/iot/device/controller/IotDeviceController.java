package com.ym.iot.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.excel.utils.ExcelBuilder;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceExportVo;
import com.ym.iot.device.domain.vo.IotDeviceVo;
import com.ym.iot.device.domain.vo.IotLatestVo;
import com.ym.iot.device.domain.vo.IotPestChartVo;
import com.ym.iot.device.domain.vo.IotPestNightChartVo;
import com.ym.iot.device.domain.vo.IotSeriesBatchVo;
import com.ym.iot.device.domain.vo.IotSeriesVo;
import com.ym.iot.device.service.IIotDataPointService;
import com.ym.iot.device.service.IIotDeviceService;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** 物联网设备档案。遥测查询由后续迁入的遥测 Controller 继续使用同一路径。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/device")
public class IotDeviceController extends BaseController {

    private static final int LATEST_BATCH_MAX_DEVICES = 100;

    private final IIotDeviceService deviceService;
    private final IIotDataPointService dataPointService;

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmConfig:query",
                "iot:alarmConfig:list"
            },
            mode = SaMode.OR)
    @GetMapping("/list")
    public R<List<IotDeviceVo>> list(IotDeviceBo bo) {
        return R.ok(deviceService.queryList(bo));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmConfig:query",
                "iot:alarmConfig:list"
            },
            mode = SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<IotDeviceVo>> page(IotDeviceBo bo, PageQuery pageQuery) {
        return R.ok(deviceService.queryPageList(bo, pageQuery));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list"
            },
            mode = SaMode.OR)
    @GetMapping("/latest/batch")
    public R<Map<Long, IotLatestVo>> getLatestBatch(
            @RequestParam String deviceIds, @RequestParam(defaultValue = "false") boolean fresh) {
        List<Long> ids = parseDeviceIds(deviceIds);
        if (ids.isEmpty()) {
            throw new ServiceException("deviceIds 不能为空");
        }
        if (ids.size() > LATEST_BATCH_MAX_DEVICES) {
            throw new ServiceException("单次最多查询 " + LATEST_BATCH_MAX_DEVICES + " 台设备");
        }
        return R.ok(dataPointService.getLatestMap(ids));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/latest")
    public R<IotLatestVo> getLatest(
            @PathVariable Long deviceId, @RequestParam(defaultValue = "false") boolean fresh) {
        return R.ok(dataPointService.getLatest(deviceId));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/series")
    public R<IotSeriesVo> getSeries(
            @PathVariable Long deviceId,
            @RequestParam("metric_code") String metricCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date to,
            @RequestParam(required = false) Integer step) {
        return R.ok(dataPointService.getSeries(deviceId, metricCode, from, to, step));
    }

    /** 批量查询同一设备的多项遥测曲线，兼容源 V1.5 前端参数。 */
    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/series-batch")
    public R<IotSeriesBatchVo> getSeriesBatch(
            @PathVariable Long deviceId,
            @RequestParam(name = "metric_code", required = false) List<String> metricCode,
            @RequestParam(name = "metric_codes", required = false) String metricCodes,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date to,
            @RequestParam(required = false) Integer step) {
        List<String> codes = resolveMetricCodes(metricCode, metricCodes);
        return R.ok(
                dataPointService.getSeriesBatch(
                        deviceId, codes.isEmpty() ? null : codes, from, to, step));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/pest-chart")
    public R<IotPestChartVo> getPestChart(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date to) {
        return R.ok(dataPointService.getPestChart(deviceId, from, to));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/pest-night-chart")
    public R<IotPestNightChartVo> getPestNightChart(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date from,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date to) {
        return R.ok(dataPointService.getPestNightChart(deviceId, from, to));
    }

    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmConfig:query",
                "iot:alarmConfig:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{deviceId}")
    public R<IotDeviceVo> getInfo(@PathVariable Long deviceId) {
        return R.ok(deviceService.queryById(deviceId));
    }

    @Log(title = "物联网设备", businessType = BusinessType.EXPORT)
    @SaCheckPermission("iot:device:export")
    @PostMapping("/export")
    public void export(IotDeviceBo bo, HttpServletResponse response) {
        List<IotDeviceExportVo> list = deviceService.queryExportList(bo);
        ExcelBuilder.of(list, IotDeviceExportVo.class).sheetName("物联网设备").toResponse(response);
    }

    private static List<Long> parseDeviceIds(String value) {
        if (StringUtils.isBlank(value)) {
            return List.of();
        }
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        for (String part : value.split(",")) {
            if (StringUtils.isBlank(part)) {
                continue;
            }
            try {
                ids.add(Long.parseLong(part.trim()));
            } catch (NumberFormatException ex) {
                throw new ServiceException("deviceIds 格式非法: " + part.trim());
            }
        }
        return new ArrayList<>(ids);
    }

    private static List<String> resolveMetricCodes(
            List<String> metricCodeParams, String metricCodesCsv) {
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        if (StringUtils.isNotBlank(metricCodesCsv)) {
            for (String part : metricCodesCsv.split(",")) {
                if (StringUtils.isNotBlank(part)) {
                    codes.add(part.trim());
                }
            }
        }
        if (metricCodeParams != null) {
            for (String part : metricCodeParams) {
                if (StringUtils.isNotBlank(part)) {
                    codes.add(part.trim());
                }
            }
        }
        return new ArrayList<>(codes);
    }
}
