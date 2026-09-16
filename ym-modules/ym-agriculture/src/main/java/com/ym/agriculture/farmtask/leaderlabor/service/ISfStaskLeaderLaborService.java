package com.ym.agriculture.farmtask.leaderlabor.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.leaderlabor.model.bo.SfStaskBatchAcceptBo;
import com.ym.agriculture.farmtask.leaderlabor.model.bo.SfStaskBatchClockInBo;
import com.ym.agriculture.farmtask.leaderlabor.model.bo.SfStaskLaborRecordQueryBo;
import com.ym.agriculture.farmtask.leaderlabor.model.bo.SfStaskLaborRecordUpdateBo;
import com.ym.agriculture.farmtask.leaderlabor.model.vo.*;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskLeaderAcceptBo;

import java.time.LocalDate;
import java.util.List;

/** 组长批量操作与计划日用工服务。 */
public interface ISfStaskLeaderLaborService {
    List<SfStaskLaborDateOptionVo> batchAcceptDateOptions();
    SfStaskBatchAcceptTasksVo batchAcceptTasks(LocalDate planDate);
    SfStaskBatchMutationVo batchAccept(SfStaskBatchAcceptBo bo);
    List<SfStaskLaborDateOptionVo> batchClockInDateOptions();
    List<SfStaskBatchTaskVo> batchClockInTasks(LocalDate planDate);
    SfStaskBatchMutationVo batchClockIn(SfStaskBatchClockInBo bo);
    int singleAccept(Long orderId, SfStaskLeaderAcceptBo bo);
    PageResult<SfStaskLaborRecordVo> page(SfStaskLaborRecordQueryBo bo, PageQuery pageQuery);
    List<SfStaskLaborLeaderOptionVo> leaders();
    SfStaskLaborRecordDetailVo detail(Long laborRecordId);
    SfStaskLaborRecordVo update(Long laborRecordId, SfStaskLaborRecordUpdateBo bo);
}
