package com.ym.agriculture.farming.algback.controller;

import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAddTaskResult;
import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.algback.model.bo.AlgBackTaskStatusBo;
import com.ym.agriculture.farming.algback.model.bo.AlgBackTaskSubmitBo;
import com.ym.agriculture.farming.algback.service.ISfAlgBackAlgorithmTaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 算法中台计算任务：提交并可选启动、更新任务状态。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/ai/algorithm-tasks")
public class SfAlgBackAlgorithmTaskController extends BaseController {

    private final ISfAlgBackAlgorithmTaskService algorithmTaskService;

    /**
     * 提交算法任务（可一并启动）
     *
     * @param bo 任务提交参数（模型号、输入资源等）
     * @return 统一响应，{@code data} 为中台返回的创建结果
     */
    @Log(title = "算法中台任务", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @PostMapping("/submit")
    public R<AlgBackAddTaskResult> submit(@Valid @RequestBody AlgBackTaskSubmitBo bo) {
        return R.ok(algorithmTaskService.submitAndOptionallyStart(bo));
    }

    /**
     * 更新算法任务状态（启停等）
     *
     * @param bo 任务号与目标状态
     * @return 统一响应，成功无 {@code data} 体
     */
    @Log(title = "算法中台任务状态", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PostMapping("/status")
    public R<Void> status(@Valid @RequestBody AlgBackTaskStatusBo bo) {
        algorithmTaskService.setTaskStatus(bo);
        return R.ok();
    }
}
