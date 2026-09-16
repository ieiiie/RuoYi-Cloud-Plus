package com.ym.agriculture.farmtask.inspection.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionCreateBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionDeleteBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionGreenhouseGroupQueryBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionHandleBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionOrderQueryBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionQueryBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionRejectBo;
import com.ym.agriculture.farmtask.inspection.model.bo.SfStaskInspectionUpdateBo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionBadgeVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionDetailVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionEmployeeOptionVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionGreenhouseGroupVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionMutationVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionOrderOptionVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionSnapshotVo;
import com.ym.agriculture.farmtask.inspection.model.vo.SfStaskInspectionListVo;

import java.util.List;

/** 农事抽检业务服务。 */
public interface ISfStaskInspectionService {

    SfStaskInspectionBadgeVo badgeCount();

    PageResult<SfStaskInspectionListVo> page(SfStaskInspectionQueryBo bo, PageQuery pageQuery);

    SfStaskInspectionDetailVo detail(Long inspectionId);

    SfStaskInspectionMutationVo create(SfStaskInspectionCreateBo bo);

    SfStaskInspectionMutationVo update(Long inspectionId, SfStaskInspectionUpdateBo bo);

    void delete(Long inspectionId, SfStaskInspectionDeleteBo bo);

    SfStaskInspectionMutationVo handle(Long inspectionId, SfStaskInspectionHandleBo bo);

    SfStaskInspectionMutationVo reject(Long inspectionId, SfStaskInspectionRejectBo bo);

    List<SfStaskInspectionGreenhouseGroupVo> greenhouseGroups(
        SfStaskInspectionGreenhouseGroupQueryBo bo);

    SfStaskInspectionSnapshotVo greenhouseSnapshot(Long greenhouseId);

    PageResult<SfStaskInspectionOrderOptionVo> orderOptions(
        SfStaskInspectionOrderQueryBo bo, PageQuery pageQuery);

    List<SfStaskInspectionEmployeeOptionVo> technicianOptions(String keyword);

    List<SfStaskInspectionEmployeeOptionVo> leaderOptions(String keyword);
}
