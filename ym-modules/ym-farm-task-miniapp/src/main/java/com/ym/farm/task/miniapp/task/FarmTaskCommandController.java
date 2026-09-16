package com.ym.farm.task.miniapp.task;

import com.ym.agriculture.api.farmtask.RemoteFarmTaskService;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskAcceptanceBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskClockInBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskCompleteBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskLeaderAcceptBo;
import com.ym.common.core.domain.R;
import jakarta.validation.Valid;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 接单、打卡、执行和验收命令适配；BFF 不开启跨服务事务。 */
@RestController
@RequestMapping("/miniapp/smart-farming/stask")
public class FarmTaskCommandController {

    @DubboReference
    private RemoteFarmTaskService farmTaskService;

    @PostMapping("/leader/orders/{orderId}/accept")
    public R<Void> leaderAccept(@PathVariable Long orderId,
                                @Valid @RequestBody RemoteTaskLeaderAcceptBo command) {
        farmTaskService.leaderAccept(orderId, command);
        return R.ok();
    }

    @PostMapping("/leader/orders/{orderId}/clock-in")
    public R<Void> clockIn(@PathVariable Long orderId,
                           @Valid @RequestBody RemoteTaskClockInBo command) {
        farmTaskService.clockIn(orderId, command);
        return R.ok();
    }

    @PostMapping("/leader/orders/{orderId}/complete")
    public R<Void> complete(@PathVariable Long orderId,
                            @Valid @RequestBody RemoteTaskCompleteBo command) {
        farmTaskService.complete(orderId, command);
        return R.ok();
    }

    @PostMapping("/leader/orders/{orderId}/reapply-acceptance")
    public R<Void> reapplyAcceptance(@PathVariable Long orderId,
                                     @Valid @RequestBody RemoteTaskCompleteBo command) {
        farmTaskService.reapplyAcceptance(orderId, command);
        return R.ok();
    }

    @PostMapping("/{role}/orders/{orderId}/acceptance")
    public R<Void> acceptance(@PathVariable String role, @PathVariable Long orderId,
                              @Valid @RequestBody RemoteTaskAcceptanceBo command) {
        String roleCode = FarmTaskWorkbenchController.roleCode(role);
        if (!"stask:production_admin".equals(roleCode) && !"stask:expert".equals(roleCode)) {
            throw new IllegalArgumentException("当前角色不能验收");
        }
        command.setAcceptorRoleCode(roleCode);
        farmTaskService.acceptance(orderId, command);
        return R.ok();
    }
}
