package com.ym.agriculture.farmtask.screen.controller;

import com.ym.common.core.domain.R;
import com.ym.agriculture.farmtask.screen.model.vo.ScreenDashboardVos;
import com.ym.agriculture.farmtask.screen.model.vo.ScreenPageVo;
import com.ym.agriculture.farmtask.screen.service.ScreenDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 真实数据大屏接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/screen")
public class ScreenDashboardController {

    private final ScreenDashboardService screenDashboardService;

    @GetMapping("/tenant")
    public R<ScreenDashboardVos.Tenant> tenant() {
        return R.ok(screenDashboardService.tenant());
    }

    @GetMapping("/greenhouses")
    public R<ScreenDashboardVos.Greenhouses> greenhouses() {
        return R.ok(screenDashboardService.greenhouses());
    }

    @GetMapping("/greenhouses/{fieldId}/photo-archives")
    public R<ScreenDashboardVos.PhotoArchives> photoArchives(@PathVariable Long fieldId) {
        return R.ok(screenDashboardService.photoArchives(fieldId));
    }

    @GetMapping("/greenhouses/{fieldId}/farm-archives/dates")
    public R<ScreenDashboardVos.FarmArchiveDates> farmArchiveDates(@PathVariable Long fieldId) {
        return R.ok(screenDashboardService.farmArchiveDates(fieldId));
    }

    @GetMapping("/greenhouses/{fieldId}/farm-archives/tasks")
    public R<ScreenDashboardVos.FarmArchiveTasks> farmArchiveTasks(@PathVariable Long fieldId,
        @RequestParam java.time.LocalDate date) {
        return R.ok(screenDashboardService.farmArchiveTasks(fieldId, date));
    }

    @GetMapping("/sops")
    public R<ScreenPageVo<ScreenDashboardVos.Sop>> sops(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "100") int pageSize) {
        return R.ok(screenDashboardService.sops(pageNum, pageSize));
    }

    @GetMapping("/sops/{sopId}")
    public R<ScreenDashboardVos.SopDetail> sopDetail(@PathVariable Long sopId) {
        return R.ok(screenDashboardService.sopDetail(sopId));
    }

    @GetMapping("/materials")
    public R<ScreenPageVo<ScreenDashboardVos.Material>> materials(
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "200") int pageSize) {
        return R.ok(screenDashboardService.materials(pageNum, pageSize));
    }

    @GetMapping("/work-orders/summary")
    public R<ScreenDashboardVos.WorkOrderSummary> workOrderSummary() {
        return R.ok(screenDashboardService.workOrderSummary());
    }

    @GetMapping("/work-orders/category-ranking")
    public R<ScreenDashboardVos.WorkOrderCategoryRanking> workOrderCategoryRanking(
        @RequestParam(defaultValue = "5") int limit) {
        return R.ok(screenDashboardService.workOrderCategoryRanking(limit));
    }

    @GetMapping("/work-orders/work-item-ranking")
    public R<ScreenDashboardVos.WorkOrderWorkItemRanking> workOrderWorkItemRanking(
        @RequestParam(defaultValue = "5") int limit) {
        return R.ok(screenDashboardService.workOrderWorkItemRanking(limit));
    }

    @GetMapping("/labor/today")
    public R<ScreenDashboardVos.TodayLabor> todayLabor() {
        return R.ok(screenDashboardService.todayLabor());
    }

    @GetMapping("/labor/cumulative")
    public R<ScreenDashboardVos.CumulativeLabor> cumulativeLabor() {
        return R.ok(screenDashboardService.cumulativeLabor());
    }

    @GetMapping("/yields/annual")
    public R<ScreenDashboardVos.AnnualYield> annualYield() {
        return R.ok(screenDashboardService.annualYield());
    }
}
