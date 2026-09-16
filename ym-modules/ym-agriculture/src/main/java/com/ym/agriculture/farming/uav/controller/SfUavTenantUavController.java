package com.ym.agriculture.farming.uav.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.uav.model.bo.SfUavAiTaskQueryBo;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightTaskQueryBo;
import com.ym.agriculture.farming.uav.model.vo.SfUavAiTaskVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightTaskVo;
import com.ym.agriculture.farming.uav.service.IUavLocalTaskQueryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;


/** 旧无人机平台历史，只访问当前租户的本地数据库。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/uav")
@cn.dev33.satoken.annotation.SaCheckLogin
@cn.dev33.satoken.annotation.SaCheckPermission("smartfarming:uav:list")
public class SfUavTenantUavController {
    private final IUavLocalTaskQueryService localTaskQueryService;
    @GetMapping("/local-flight-tasks/page")
    public R<PageResult<SfUavFlightTaskVo>> pageLocalFlightTasks(SfUavFlightTaskQueryBo bo, PageQuery pageQuery) {
        return R.ok(localTaskQueryService.pageFlightTasks(bo != null ? bo : new SfUavFlightTaskQueryBo(), pageQuery));
    }

    @GetMapping("/local-flight-tasks/{id}")
    public R<SfUavFlightTaskVo> getLocalFlightTask(@PathVariable Long id) {
        return R.ok(localTaskQueryService.getFlightTask(id));
    }

    @GetMapping("/ai-tasks/page")
    public R<PageResult<SfUavAiTaskVo>> pageLocalAiTasks(SfUavAiTaskQueryBo bo, PageQuery pageQuery) {
        return R.ok(localTaskQueryService.pageAiTasks(bo != null ? bo : new SfUavAiTaskQueryBo(), pageQuery));
    }

    @GetMapping("/ai-tasks/{id}")
    public R<SfUavAiTaskVo> getLocalAiTask(@PathVariable Long id) {
        return R.ok(localTaskQueryService.getAiTask(id));
    }

    @GetMapping("/ai-tasks/by-uav-job-id")
    public R<SfUavAiTaskVo> getLocalAiTaskByUavJobId(@RequestParam("uavJobId") String uavJobId) {
        return R.ok(localTaskQueryService.getAiTaskByUavJobId(uavJobId));
    }
}
