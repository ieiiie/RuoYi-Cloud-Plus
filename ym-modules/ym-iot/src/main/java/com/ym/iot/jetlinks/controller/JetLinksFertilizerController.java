package com.ym.iot.jetlinks.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.web.core.BaseController;
import com.ym.iot.fertilizer.domain.bo.FertilizerResetBo;
import com.ym.iot.fertilizer.domain.bo.FertilizerTankParamBo;
import com.ym.iot.fertilizer.domain.dto.FertilizerRuntimeConfig;
import com.ym.iot.fertilizer.domain.vo.FertilizerStateSnapshot;
import com.ym.iot.fertilizer.domain.vo.TaskInfo;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksFertilizerService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 施肥机控制 REST 接口。
 *
 * <h3>接口模式</h3>
 *
 * <ul>
 *   <li><b>查询类（同步）</b>：GET/POST 直接返回结果，毫秒~15 秒内响应。
 *   <li><b>控制类（异步）</b>：POST 返回 {@code { taskId: "xxx" }}， 前端轮询 {@code GET
 *       /api/fertilizer/task/{taskId}} 直到 {@code status} 为 {@code SUCCEEDED} 或 {@code FAILED}。
 * </ul>
 *
 * <h3>多用户并发</h3>
 *
 * 同一设备同一时间仅一个控制流程执行。后续请求收到 {@code ServiceException("设备正有「xxx」任务执行中")} 或 {@code
 * ServiceException("设备正有其他控制任务进行中")}。 紧急停止不受此限制。
 *
 * @author ym-cloud
 */
@Validated
@RestController
@RequestMapping("/fertilizer")
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksFertilizerController extends BaseController {

    private final IJetLinksFertilizerService controllerService;

    // ======================== 查询接口（同步） ========================

    /**
     * 查询设备最新状态快照。
     *
     * <p>内存中已有该设备快照时毫秒级返回；尚无快照时（如进程冷启动后首次请求）， 服务端会主动下发 GET_REALTIME(0x0B) 并阻塞等待首帧，最长约 15s，
     * 超时或命令失败由全局异常处理返回业务错误（与 POST {@code /realtime} 异步任务中的等待上限一致）。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 状态快照，含 {@code state}（主状态）、{@code subStage}（运行子阶段）、 {@code workMode}（自动/手动）、{@code
     *     riskLevel}（风险等级）、 {@code status1Flags}~{@code status4Flags}（状态字按位解析）、 总体测点（{@code
     *     flow}/{@code pressure}/{@code voltage}/{@code runTime}/{@code remainDelayTime}/{@code
     *     pumpFreq}、 {@code liquidLevel1}/{@code liquidLevel2}/{@code liquidLevel3}）、 {@code
     *     alarmFlags}（已展开的中文报警名）等
     */
    @SaCheckPermission(
            value = {"iot:fertilizer:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/state")
    public R<FertilizerStateSnapshot> getState(@PathVariable Long deviceId) {
        return R.ok(controllerService.getState(deviceId));
    }

    /**
     * 主动向设备下发一次 GET_REALTIME 命令并等待返回。 用于首次打开页面或怀疑状态过期时强制刷新。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 异步任务信息（等待 ≤15s）
     */
    @SaCheckPermission("iot:fertilizer:read")
    @PostMapping("/{deviceId}/realtime")
    public R<TaskInfo> refreshRealtime(@PathVariable Long deviceId) {
        return R.ok(controllerService.refreshRealtime(deviceId));
    }

    /**
     * 查询施肥机运行配置。
     *
     * @return 运行配置，包含测试模式开关
     */
    @SaCheckPermission(
            value = {"iot:fertilizer:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/runtime-config")
    public R<FertilizerRuntimeConfig> runtimeConfig() {
        return R.ok(controllerService.runtimeConfig());
    }

    // ======================== 控制接口（异步） ========================

    /**
     * 一键启动施肥。
     *
     * <p>完整流程：拉实时 → 严格前置检查 → 启动 → 等待 RUNNING。 启动接口不下发参数、不自动复位；启动前参数读取和用户确认由前端完成。
     *
     * <p><b>前置条件</b>：设备在线、自动模式、非故障/急停态、无关键报警； 启动前 status3.bit0=0（无需复位），status3.bit0=1 表示需要先复位。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param tanks 兼容旧客户端请求体；服务端启动流程会忽略该参数，不做参数下发。
     * @return 异步任务信息，含 taskId 供轮询进度
     */
    @SaCheckPermission("iot:fertilizer:start")
    @PostMapping("/{deviceId}/start")
    public R<TaskInfo> start(
            @PathVariable Long deviceId,
            @RequestBody(required = false) List<FertilizerTankParamBo> tanks) {
        return R.ok(controllerService.start(deviceId, tanks));
    }

    /**
     * 有序停止当前施肥作业。
     *
     * <p>设备收到 STOP 后会先完成自动冲洗，再进入待机状态。
     *
     * <p><b>前置条件</b>：设备在线（允许在任何状态下发 STOP）
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:stop")
    @PostMapping("/{deviceId}/stop")
    public R<TaskInfo> stop(@PathVariable Long deviceId) {
        return R.ok(controllerService.stop(deviceId));
    }

    /**
     * 紧急停止。
     *
     * <p>立即关闭所有设备，不执行反洗和冲洗。跳过设备锁和前置检查。 急停后设备进入 EMERGENCY 状态，必须人工现场确认安全并复位后才能重新启动。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param reason 紧急停止原因（必填），用于审计日志
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:emergencyStop")
    @PostMapping("/{deviceId}/emergency-stop")
    public R<TaskInfo> emergencyStop(@PathVariable Long deviceId, @RequestParam String reason) {
        return R.ok(controllerService.emergencyStop(deviceId, reason));
    }

    /**
     * 复位设备。
     *
     * <p>清除上一轮异常残留状态，重新自检液位/流量/压力/泵等条件， 设备进入 READY 后可启动。
     *
     * <p><b>前置条件</b>：设备在线，且不在运行态（施肥中禁止复位）
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param bo 复位选项；continuePending=true 时先把未完成施肥量写回预计施肥量
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:reset")
    @PostMapping("/{deviceId}/reset")
    public R<TaskInfo> reset(
            @PathVariable Long deviceId, @RequestBody(required = false) FertilizerResetBo bo) {
        return R.ok(controllerService.reset(deviceId, bo));
    }

    /**
     * 无复位直接启动（高风险命令）。
     *
     * <p>跳过标准复位保护，直接下发启动。仅用于调试验证， 普通前端流程不应开放此能力。需显式 confirmRisk。
     *
     * <p><b>前置条件</b>：设备在线、自动模式、非故障/急停态/运行态、无关键报警 （<b>不检查</b>复位状态）
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:startWithoutReset")
    @PostMapping("/{deviceId}/start-without-reset")
    public R<TaskInfo> startWithoutReset(@PathVariable Long deviceId) {
        return R.ok(controllerService.startWithoutReset(deviceId));
    }

    /**
     * 读取设备当前所有参数（三罐作业 + 密度/搅拌）。
     *
     * <p>主动下发 GET_REALTIME(0x0B) + GET_SETTINGS(0x0C) 到设备， 等待两帧回包后返回含完整参数的状态快照。
     * 前端拿到数据后展示并供用户编辑，编辑完成后调用 {@code POST /param/apply} 下发。
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 异步任务信息，等待 ≤20s。成功后调用 GET /state 获取含参数的状态快照
     */
    @SaCheckPermission("iot:fertilizer:read")
    @PostMapping("/{deviceId}/param/read")
    public R<TaskInfo> readCurrentParams(@PathVariable Long deviceId) {
        return R.ok(controllerService.readCurrentParams(deviceId));
    }

    /**
     * 参数下发并回读验证。
     *
     * <p>按组下发：A组(100~111 单次作业) → C组(121~123 累计量)。 下发完成后通过 GET_REALTIME(0x0B) 读回 set1/set2/set3 做验证。
     *
     * <p><b>特殊规则</b>：amount 为空跳过该罐不下发，amount=0 时按零量参数完整下发并要求设备回读 0；其他字段为 null 时跳过对应参数项； flow 范围为
     * 300*密度 ~ 1500*密度；time 为设备派生字段
     *
     * @param deviceId 设备主键（iot_device.id）
     * @param tanks 三罐参数列表
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:paramApply")
    @RepeatSubmit(interval = 30000)
    @PostMapping("/{deviceId}/param/apply")
    public R<TaskInfo> applyParams(
            @PathVariable Long deviceId, @RequestBody List<FertilizerTankParamBo> tanks) {
        return R.ok(controllerService.applyParams(deviceId, tanks));
    }

    /**
     * 加引水（按需执行，非施肥必须步骤）。
     *
     * <p>用于施肥泵及管路排空气，仅在管道含气/首次启动/长时间停机后使用。 正常施肥无需执行。
     *
     * <p><b>前置条件</b>：设备在线，且不在运行态
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:primeWater")
    @PostMapping("/{deviceId}/prime-water")
    public R<TaskInfo> primeWater(@PathVariable Long deviceId) {
        return R.ok(controllerService.primeWater(deviceId));
    }

    /**
     * 清罐。
     *
     * <p>设备执行 3 次清罐循环后自动停机，总耗时较长（默认超时 600s）。
     *
     * <p><b>前置条件</b>：设备在线，且不在运行态
     *
     * @param deviceId 设备主键（iot_device.id）
     * @return 异步任务信息
     */
    @SaCheckPermission("iot:fertilizer:cleanTank")
    @PostMapping("/{deviceId}/clean-tank")
    public R<TaskInfo> cleanTank(@PathVariable Long deviceId) {
        return R.ok(controllerService.cleanTank(deviceId));
    }

    // ======================== 任务查询（同步） ========================

    /**
     * 查询异步任务进度。
     *
     * @param taskId 控制接口返回的任务 ID
     * @return 任务信息，含 {@code status}（QUEUED/RUNNING/SUCCEEDED/FAILED）、 {@code step}（当前步骤描述）、{@code
     *     progress}（0~100）、 {@code result}（成功文本）或 {@code error}（失败原因）
     */
    @SaCheckPermission(
            value = {"iot:fertilizer:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/task/{taskId}")
    public R<TaskInfo> getTask(@PathVariable String taskId) {
        return R.ok(controllerService.getTask(taskId));
    }

    /**
     * 查询设备当前活跃任务（QUEUED 或 RUNNING）。
     *
     * @param deviceId 设备主键
     * @return 活跃任务；无活跃任务时 data 为 null
     */
    @SaCheckPermission(
            value = {"iot:fertilizer:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/{deviceId}/task/active")
    public R<TaskInfo> getActiveTask(@PathVariable Long deviceId) {
        return R.ok(controllerService.getActiveTask(deviceId));
    }
}
