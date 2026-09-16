package com.ym.iot.alarm.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.iot.alarm.service.INativeAlarmService;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.jetlinks.rpc.RecordDto;

import org.springframework.web.bind.annotation.*;

import java.util.*;

/** Business UI API backed exclusively by native JetLinks services. */
@RestController
@RequestMapping("/iot/alarm")
@ConditionalOnJetLinks
public class NativeAlarmController {
    private final INativeAlarmService service;

    public NativeAlarmController(INativeAlarmService service) {
        this.service = service;
    }

    public record EnableRequest(boolean enabled, long version) {}

    @GetMapping("/config/page")
    @SaCheckPermission(
            value = {"iot:alarmConfig:list", "iot:alarmConfig:query"},
            mode = SaMode.OR)
    public R<PageResult<Map<String, Object>>> configurations(
            @RequestParam Map<String, String> query) {
        return R.ok(service.configurations(query));
    }

    @GetMapping("/config/{id}")
    @SaCheckPermission(
            value = {"iot:alarmConfig:list", "iot:alarmConfig:query"},
            mode = SaMode.OR)
    public R<Map<String, Object>> configuration(@PathVariable("id") String id) {
        return R.ok(service.configuration(id));
    }

    @GetMapping("/config/{id}/settings")
    @SaCheckPermission(
            value = {"iot:alarmConfig:list", "iot:alarmConfig:query"},
            mode = SaMode.OR)
    public R<Map<String, Object>> settings(@PathVariable("id") String id) {
        return R.ok(service.settings(id));
    }

    @PatchMapping("/config/{id}/settings")
    @SaCheckPermission("iot:alarmConfig:edit")
    @Log(title = "租户报警设置", businessType = BusinessType.UPDATE)
    public R<Map<String, Object>> updateSettings(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Request-Id") String requestId) {
        return R.ok(service.updateSettings(id, body, requestId));
    }

    @PostMapping("/config")
    @SaCheckPermission("iot:alarmConfig:add")
    @Log(title = "JetLinks告警配置", businessType = BusinessType.INSERT)
    public R<Map<String, Object>> create(
            @RequestBody RecordDto body, @RequestHeader("X-Request-Id") String requestId) {
        return R.ok(service.save(null, body, requestId));
    }

    @PutMapping("/config/{id}")
    @SaCheckPermission("iot:alarmConfig:edit")
    @Log(title = "JetLinks告警配置", businessType = BusinessType.UPDATE)
    public R<Map<String, Object>> save(
            @PathVariable("id") String id,
            @RequestBody RecordDto body,
            @RequestHeader("X-Request-Id") String requestId) {
        return R.ok(service.save(id, body, requestId));
    }

    @PutMapping("/config/{id}/enabled")
    @SaCheckPermission("iot:alarmConfig:edit")
    @Log(title = "JetLinks告警启停", businessType = BusinessType.UPDATE)
    public R<Map<String, Object>> enabled(
            @PathVariable("id") String id,
            @RequestBody EnableRequest body,
            @RequestHeader("X-Request-Id") String requestId) {
        return R.ok(service.enabled(id, body.enabled(), body.version(), requestId));
    }

    @DeleteMapping("/config/{id}")
    @SaCheckPermission("iot:alarmConfig:remove")
    @Log(title = "JetLinks告警配置", businessType = BusinessType.DELETE)
    public R<Boolean> delete(
            @PathVariable("id") String id,
            @RequestParam("version") long version,
            @RequestHeader("X-Request-Id") String requestId) {
        return R.ok(service.delete(id, version, requestId));
    }

    @GetMapping("/record/page")
    @SaCheckPermission(
            value = {"iot:alarmRecord:list", "iot:alarmRecord:query"},
            mode = SaMode.OR)
    public R<PageResult<Map<String, Object>>> records(@RequestParam Map<String, String> query) {
        return R.ok(service.records(query));
    }

    @GetMapping("/record/{id}")
    @SaCheckPermission(
            value = {"iot:alarmRecord:list", "iot:alarmRecord:query"},
            mode = SaMode.OR)
    public R<Map<String, Object>> record(@PathVariable("id") String id) {
        return R.ok(service.record(id));
    }

    @GetMapping("/history/page")
    @SaCheckPermission(
            value = {"iot:alarmRecord:list", "iot:alarmRecord:query"},
            mode = SaMode.OR)
    public R<PageResult<Map<String, Object>>> history(@RequestParam Map<String, String> query) {
        return R.ok(service.history(query));
    }

    @GetMapping("/handle-history/page")
    @SaCheckPermission(
            value = {"iot:alarmRecord:list", "iot:alarmRecord:query"},
            mode = SaMode.OR)
    public R<PageResult<Map<String, Object>>> handlingHistory(
            @RequestParam Map<String, String> query) {
        return R.ok(service.handlingHistory(query));
    }

    @PostMapping("/record/{id}/handle")
    @SaCheckPermission("iot:alarmRecord:handle")
    @Log(title = "JetLinks告警处理", businessType = BusinessType.UPDATE)
    public R<Map<String, Object>> handle(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> body,
            @RequestHeader("X-Request-Id") String requestId) {
        return R.ok(service.handle(id, body, requestId));
    }

    @GetMapping("/notification/options")
    @SaCheckPermission(
            value = {
                "iot:alarmConfig:list",
                "iot:alarmConfig:query",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmRecord:handle",
                "iot:alarmRecord:list",
                "iot:alarmRecord:query"
            },
            mode = SaMode.OR)
    public R<List<Map<String, Object>>> notificationOptions() {
        return R.ok(service.notificationOptions());
    }
}
