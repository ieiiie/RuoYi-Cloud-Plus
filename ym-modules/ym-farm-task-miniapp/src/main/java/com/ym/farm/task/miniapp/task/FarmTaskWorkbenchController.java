package com.ym.farm.task.miniapp.task;

import com.ym.agriculture.api.farmtask.RemoteFarmTaskService;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteTaskViewVo;
import com.ym.common.core.domain.R;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 保留生产管理员、技术员和组长工作台的历史移动端路径。 */
@RestController
@RequestMapping("/miniapp/smart-farming/stask")
@RequiredArgsConstructor
public class FarmTaskWorkbenchController {

    @DubboReference
    private RemoteFarmTaskService farmTaskService;

    @GetMapping("/{role}/workbench")
    public R<RemoteTaskViewVo> workbench(@PathVariable String role) {
        return R.ok(farmTaskService.workbench(roleCode(role)));
    }

    @GetMapping("/{role}/workbench/summary")
    public R<RemoteTaskViewVo> summary(@PathVariable String role) {
        return R.ok(farmTaskService.workbenchSummary(roleCode(role)));
    }

    @GetMapping("/{role}/workbench/tasks")
    public R<RemoteTaskViewVo> tasks(@PathVariable String role, @RequestParam String tab) {
        return R.ok(farmTaskService.workbenchTasks(roleCode(role), tab));
    }

    @GetMapping("/{role}/orders/{orderId}")
    public R<RemoteTaskViewVo> detail(@PathVariable String role, @PathVariable Long orderId) {
        roleCode(role);
        return R.ok(farmTaskService.detail(orderId));
    }

    static String roleCode(String role) {
        return switch (role) {
            case "manager" -> "stask:production_admin";
            case "technician" -> "stask:expert";
            case "leader" -> "stask:group_leader";
            default -> throw new IllegalArgumentException("不支持的农事任务角色路径: " + role);
        };
    }
}
