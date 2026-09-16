package com.ym.agriculture.farming.uav;

import com.ym.agriculture.farming.algback.dao.SfAiInferenceLogMapper;
import com.ym.agriculture.farming.uav.controller.SfUavFlightPlanController;
import com.ym.agriculture.farming.uav.controller.SfUavTenantUavController;
import com.ym.agriculture.farming.uav.dao.*;
import com.ym.agriculture.farming.uav.model.entity.*;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightTaskVo;
import com.ym.agriculture.farming.uav.service.impl.*;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.system.api.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UavHistoryBoundaryTest {
    private LoginUser login(String tenant) {
        LoginUser user = new LoginUser(); user.setTenantId(tenant); user.setUserId(1L); return user;
    }
    @Test void historyControllersExposeOnlyLocalReads() {
        for (Class<?> type : List.of(SfUavFlightPlanController.class, SfUavTenantUavController.class)) {
            assertEquals("smartfarming:uav:list", type.getAnnotation(cn.dev33.satoken.annotation.SaCheckPermission.class).value()[0]);
            for (var method : type.getDeclaredMethods()) {
                if (method.isSynthetic()) continue;
                assertNotNull(method.getAnnotation(GetMapping.class), method.toString());
            }
        }
    }
    @Test void planDetailRejectsDifferentHistoricalTenantBeforeLoadingExecutions() {
        var plans = mock(SfUavFlightPlanMapper.class); var executions = mock(SfUavFlightPlanExecutionMapper.class);
        SfUavFlightPlan row = new SfUavFlightPlan(); row.setPlanId(9L); row.setTenantId("original");
        when(plans.selectById(9L)).thenReturn(row);
        try (var auth = mockStatic(LoginHelper.class)) {
            auth.when(LoginHelper::getLoginUser).thenReturn(login("new-owner"));
            assertThrows(ServiceException.class, () -> new SfUavFlightPlanServiceImpl(plans, executions).getPlan(9L));
            verifyNoInteractions(executions);
        }
    }
    @Test void historicalTaskDoesNotFollowCurrentDeviceOwnership() {
        var flights = mock(SfUavFlightTaskMapper.class); var media = mock(SfUavMediaFileMapper.class);
        var service = new UavLocalTaskQueryServiceImpl(flights, mock(SfUavAiTaskMapper.class), mock(SfAiInferenceLogMapper.class), media);
        SfUavFlightTask row = new SfUavFlightTask(); row.setId(9L); row.setTenantId("original"); row.setStatus(1); row.setUavJobId("job");
        when(flights.selectById(9L)).thenReturn(row);
        try (var auth = mockStatic(LoginHelper.class)) {
            auth.when(LoginHelper::getLoginUser).thenReturn(login("new-owner"));
            assertThrows(ServiceException.class, () -> service.getFlightTask(9L));
            verifyNoInteractions(media);
        }
    }
    @Test void listQueriesExplicitlyRestrictTheOriginalTenant() {
        var flights = mock(SfUavFlightTaskMapper.class);
        var service = new UavLocalTaskQueryServiceImpl(flights, mock(SfUavAiTaskMapper.class), mock(SfAiInferenceLogMapper.class), mock(SfUavMediaFileMapper.class));
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "history-test"), SfUavFlightTask.class);
        when(flights.buildLocalPageWrapper(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.isNull())).thenCallRealMethod();
        when(flights.selectPage(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SfUavFlightTask> query = invocation.getArgument(1);
            assertTrue(query.getSqlSegment().contains("tenant_id"));
            assertTrue(query.getParamNameValuePairs().containsValue("original"));
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<SfUavFlightTask> page = invocation.getArgument(0);
            page.setRecords(List.of()); return page;
        });
        try (var auth = mockStatic(LoginHelper.class)) {
            auth.when(LoginHelper::getLoginUser).thenReturn(login("original"));
            assertEquals(0, service.pageFlightTasks(new com.ym.agriculture.farming.uav.model.bo.SfUavFlightTaskQueryBo(), new com.ym.common.mybatis.core.page.PageQuery(10, 1)).getTotal());
        }
    }
    @Test void missingTenantCannotReadHistoricalPlans() {
        try (var auth = mockStatic(LoginHelper.class)) {
            auth.when(LoginHelper::getLoginUser).thenReturn(null);
            var plans = mock(SfUavFlightPlanMapper.class);
            assertThrows(ServiceException.class, () -> new SfUavFlightPlanServiceImpl(plans, mock(SfUavFlightPlanExecutionMapper.class)).listPlans(9L));
            verifyNoInteractions(plans);
        }
    }
}
