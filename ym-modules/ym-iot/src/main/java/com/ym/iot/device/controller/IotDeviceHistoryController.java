package com.ym.iot.device.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.excel.utils.ExcelBuilder;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDataPointRecordVo;
import com.ym.iot.device.domain.vo.IotDeviceHistoryVo;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksTelemetryService;
import com.ym.iot.ownership.service.impl.DeviceHistoryAccessService;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.apache.fesod.sheet.annotation.ExcelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;

/** 设备管理及历史详情专用接口；通用 /list 和操作设备选择保持当前归属语义。 */
@RestController
@RequiredArgsConstructor
@ConditionalOnJetLinks
@RequestMapping("/iot/device")
public class IotDeviceHistoryController extends BaseController {
    private final DeviceHistoryAccessService history;
    private final IJetLinksTelemetryService telemetry;

    @GetMapping("/visible/list")
    @SaCheckPermission(
            value = {"iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    public R<PageResult<IotDeviceHistoryVo>> visible(
            IotDeviceBo bo,
            PageQuery page,
            @RequestParam(defaultValue = "ALL") String ownershipStatus) {
        return R.ok(history.visible(bo, page, ownershipStatus));
    }

    @GetMapping("/{deviceId}/history-detail")
    @SaCheckPermission(
            value = {
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "iot:video:list"
            },
            mode = SaMode.OR)
    public R<IotDeviceHistoryVo> detail(@PathVariable Long deviceId) {
        return R.ok(history.detail(deviceId));
    }

    /** 不传采集时间时返回本租户可见的最后一条记录。 */
    @GetMapping("/{deviceId}/record")
    @SaCheckPermission(
            value = {"iot:device:list", "iot:device:query", "iot:monitor:list"},
            mode = SaMode.OR)
    public R<IotDataPointRecordVo> record(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date collectTime,
            @RequestParam(required = false) Long collectTimestamp) {
        return R.ok(telemetry.getRecord(deviceId, time(collectTime, collectTimestamp)));
    }

    @GetMapping("/{deviceId}/record/adjacent")
    @SaCheckPermission(
            value = {"iot:device:list", "iot:device:query", "iot:monitor:list"},
            mode = SaMode.OR)
    public R<IotDataPointRecordVo> adjacent(
            @PathVariable Long deviceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                    Date collectTime,
            @RequestParam(required = false) Long collectTimestamp,
            @RequestParam boolean previous) {
        return R.ok(
                telemetry.getAdjacentRecord(
                        deviceId, time(collectTime, collectTimestamp), previous));
    }

    private static Date time(Date text, Long timestamp) {
        if (text != null && timestamp != null && text.getTime() != timestamp)
            throw new com.ym.common.core.exception.ServiceException("采集时间参数不一致");
        return timestamp == null ? text : new Date(timestamp);
    }

    /** 使用同一历史授权目录导出，不混入设备转出后的状态和位置。 */
    @PostMapping("/visible/export")
    @SaCheckPermission("iot:device:export")
    @Log(title = "设备归属目录", businessType = BusinessType.EXPORT)
    public void export(
            IotDeviceBo bo,
            @RequestParam(defaultValue = "ALL") String ownershipStatus,
            HttpServletResponse response) {
        var rows = new ArrayList<HistoryDeviceExport>();
        for (int number = 1; ; number++) {
            var page = history.visible(bo, new PageQuery(500, number), ownershipStatus);
            for (var row : page.getRows()) {
                var out = new HistoryDeviceExport();
                out.deviceCode = row.getDeviceCode();
                out.deviceName = row.getDeviceName();
                out.productName = row.getProductName();
                out.ownership = "CURRENT".equals(row.getOwnershipStatus()) ? "当前归属" : "已转出";
                out.periods =
                        row.getOwnershipPeriods().stream()
                                .map(
                                        p ->
                                                java.time.Instant.ofEpochMilli(p.from())
                                                        + " ~ "
                                                        + (p.to() == null
                                                                ? "至今"
                                                                : java.time.Instant.ofEpochMilli(
                                                                        p.to())))
                                .collect(java.util.stream.Collectors.joining("；"));
                rows.add(out);
            }
            if (page.getRows().isEmpty() || (long) number * 500 >= page.getTotal()) break;
        }
        ExcelBuilder.of(rows, HistoryDeviceExport.class).sheetName("设备目录").toResponse(response);
    }

    @lombok.Data
    public static class HistoryDeviceExport {
        @ExcelProperty("设备编号")
        private String deviceCode;

        @ExcelProperty("设备名称")
        private String deviceName;

        @ExcelProperty("产品名称")
        private String productName;

        @ExcelProperty("归属状态")
        private String ownership;

        @ExcelProperty("本租户使用期间（UTC）")
        private String periods;
    }
}
