package com.ym.agriculture.farming.uav.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightPlanQueryBo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanDetailVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightPlanVo;

import java.util.List;

public interface ISfUavFlightPlanService {
    List<SfUavFlightPlanVo> listPlans(Long plantingBatchId);
    PageResult<SfUavFlightPlanVo> pagePlans(SfUavFlightPlanQueryBo bo, PageQuery pageQuery);
    SfUavFlightPlanDetailVo getPlan(Long planId);
}
