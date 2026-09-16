package com.ym.agriculture.farmtask.yieldrecord.service;

import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldBatchCreateBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldQueryBo;
import com.ym.agriculture.farmtask.yieldrecord.model.bo.SfStaskYieldUpdateBo;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskAnnualYieldRowVo;
import com.ym.agriculture.farmtask.yieldrecord.model.vo.SfStaskYieldRecordVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * 产量记录服务。
 */
public interface ISfStaskYieldRecordService {

    PageResult<SfStaskYieldRecordVo> page(SfStaskYieldQueryBo bo, PageQuery pageQuery);

    SfStaskYieldRecordVo getById(Long yieldId);

    List<Long> batchCreate(SfStaskYieldBatchCreateBo bo);

    void update(Long yieldId, SfStaskYieldUpdateBo bo);

    void remove(Collection<Long> yieldIds);

    List<SfStaskAnnualYieldRowVo> annualYield(LocalDate startDate, LocalDate endDate);
}
