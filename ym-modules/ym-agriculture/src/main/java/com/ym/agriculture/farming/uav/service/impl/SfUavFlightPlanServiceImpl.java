package com.ym.agriculture.farming.uav.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.uav.dao.SfUavFlightPlanExecutionMapper;
import com.ym.agriculture.farming.uav.dao.SfUavFlightPlanMapper;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightPlanQueryBo;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightPlan;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightPlanExecution;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanDetailVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanExecutionVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanVo;
import com.ym.agriculture.farming.uav.service.ISfUavFlightPlanService;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class SfUavFlightPlanServiceImpl implements ISfUavFlightPlanService {
    private final SfUavFlightPlanMapper planMapper;
    private final SfUavFlightPlanExecutionMapper executionMapper;

@Override
    public List<SfUavFlightPlanVo> listPlans(Long plantingBatchId) {
        if (plantingBatchId == null) {
            throw new ServiceException("plantingBatchId is required");
        }
        requireLoginTenant();
        return planMapper.selectList(Wrappers.<SfUavFlightPlan>lambdaQuery()
            .eq(SfUavFlightPlan::getTenantId, requireLoginTenant()).eq(SfUavFlightPlan::getPlantingBatchId, plantingBatchId)
            .orderByDesc(SfUavFlightPlan::getCreateTime)).stream()
            .map(this::toVo)
            .toList();
    }

@Override
    public PageResult<SfUavFlightPlanVo> pagePlans(SfUavFlightPlanQueryBo bo, PageQuery pageQuery) {
        requireLoginTenant();
        if (bo == null) {
            bo = new SfUavFlightPlanQueryBo();
        }
        LambdaQueryWrapper<SfUavFlightPlan> w = Wrappers.<SfUavFlightPlan>lambdaQuery().eq(SfUavFlightPlan::getTenantId, requireLoginTenant());
        if (bo.getPlantingBatchId() != null) {
            w.eq(SfUavFlightPlan::getPlantingBatchId, bo.getPlantingBatchId());
        }
        if (bo.getFieldId() != null) {
            w.eq(SfUavFlightPlan::getFieldId, bo.getFieldId());
        }
        if (StringUtils.hasText(bo.getPlanMode())) {
            w.eq(SfUavFlightPlan::getPlanMode, upper(bo.getPlanMode()));
        }
        if (StringUtils.hasText(bo.getStatus())) {
            w.eq(SfUavFlightPlan::getStatus, upper(bo.getStatus()));
        }
        if (StringUtils.hasText(bo.getPlanName())) {
            w.like(SfUavFlightPlan::getPlanName, bo.getPlanName().trim());
        }
        if (bo.getCreateTimeBegin() != null) {
            w.ge(SfUavFlightPlan::getCreateTime, bo.getCreateTimeBegin());
        }
        if (bo.getCreateTimeEnd() != null) {
            w.le(SfUavFlightPlan::getCreateTime, bo.getCreateTimeEnd());
        }
        w.orderByDesc(SfUavFlightPlan::getCreateTime);
        Page<SfUavFlightPlan> page = planMapper.selectPage(pageQuery.build(), w);
        List<SfUavFlightPlanVo> rows = page.getRecords().stream().map(this::toVo).toList();
        Page<SfUavFlightPlanVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(rows);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(voPage);
    }

@Override
    public SfUavFlightPlanDetailVo getPlan(Long planId) {
        SfUavFlightPlan plan = requirePlan(planId);
        SfUavFlightPlanDetailVo vo = BeanUtil.copyProperties(toVo(plan), SfUavFlightPlanDetailVo.class);
        List<SfUavFlightPlanExecutionVo> executions = executionMapper.selectList(Wrappers.<SfUavFlightPlanExecution>lambdaQuery()
            .eq(SfUavFlightPlanExecution::getTenantId, requireLoginTenant()).eq(SfUavFlightPlanExecution::getPlanId, planId)
            .orderByDesc(SfUavFlightPlanExecution::getOccurrenceAt)).stream()
            .map(e -> MapstructUtils.convert(e, SfUavFlightPlanExecutionVo.class))
            .toList();
        vo.setExecutions(executions);
        return vo;
    }

    private SfUavFlightPlan requirePlan(Long planId) {
        if (planId == null) {
            throw new ServiceException("planId is required");
        }
        requireLoginTenant();
        SfUavFlightPlan plan = planMapper.selectById(planId);
        if (plan == null || !requireLoginTenant().equals(plan.getTenantId())) {
            throw new ServiceException("flight plan does not exist");
        }
        return plan;
    }

    private String requireLoginTenant() {
        if (LoginHelper.getLoginUser() == null || !StringUtils.hasText(LoginHelper.getLoginUser().getTenantId())) {
            throw new ServiceException("current login has no tenant");
        }
        return LoginHelper.getLoginUser().getTenantId().trim();
    }

    private SfUavFlightPlanVo toVo(SfUavFlightPlan plan) {
        return MapstructUtils.convert(plan, SfUavFlightPlanVo.class);
    }

    private static String upper(String text) {
        return StringUtils.hasText(text) ? text.trim().toUpperCase(Locale.ROOT) : "";
    }
}
