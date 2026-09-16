package com.ym.agriculture.farmtask.screen.service;

import com.ym.agriculture.farmtask.screen.model.vo.ScreenDashboardVos;
import com.ym.agriculture.farmtask.screen.model.vo.ScreenPageVo;

/** 真实数据大屏查询服务。 */
public interface ScreenDashboardService {
    ScreenDashboardVos.Tenant tenant();
    ScreenDashboardVos.Greenhouses greenhouses();
    ScreenDashboardVos.PhotoArchives photoArchives(Long fieldId);
    ScreenDashboardVos.FarmArchiveDates farmArchiveDates(Long fieldId);
    ScreenDashboardVos.FarmArchiveTasks farmArchiveTasks(Long fieldId, java.time.LocalDate archiveDate);
    ScreenPageVo<ScreenDashboardVos.Sop> sops(int pageNum, int pageSize);
    ScreenDashboardVos.SopDetail sopDetail(Long sopId);
    ScreenPageVo<ScreenDashboardVos.Material> materials(int pageNum, int pageSize);
    ScreenDashboardVos.WorkOrderSummary workOrderSummary();
    ScreenDashboardVos.WorkOrderCategoryRanking workOrderCategoryRanking(int limit);
    ScreenDashboardVos.WorkOrderWorkItemRanking workOrderWorkItemRanking(int limit);
    ScreenDashboardVos.TodayLabor todayLabor();
    ScreenDashboardVos.CumulativeLabor cumulativeLabor();
    ScreenDashboardVos.AnnualYield annualYield();
}
