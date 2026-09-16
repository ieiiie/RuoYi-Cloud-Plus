package com.ym.agriculture.farming.uav.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightPlanQueryBo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanDetailVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanVo;
import com.ym.agriculture.farming.uav.service.ISfUavFlightPlanService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 旧无人机平台已停用，仅查询本地历史飞行计划。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/uav")
@cn.dev33.satoken.annotation.SaCheckLogin
@cn.dev33.satoken.annotation.SaCheckPermission("smartfarming:uav:list")
public class SfUavFlightPlanController {
    private final ISfUavFlightPlanService flightPlanService;
    @GetMapping("/flight-plans")
    public R<List<SfUavFlightPlanVo>> listFlightPlans(@RequestParam Long plantingBatchId) {
        return R.ok(flightPlanService.listPlans(plantingBatchId));
    }
    @GetMapping("/flight-plans/page")
    public R<PageResult<SfUavFlightPlanVo>> pageFlightPlans(SfUavFlightPlanQueryBo bo, PageQuery pageQuery) {
        return R.ok(flightPlanService.pagePlans(bo, pageQuery));
    }
    @GetMapping("/flight-plans/{id}")
    public R<SfUavFlightPlanDetailVo> getFlightPlan(@PathVariable Long id) {
        return R.ok(flightPlanService.getPlan(id));
    }
}
