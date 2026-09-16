package com.ym.agriculture.farmtask.dubbo;

import com.alibaba.fastjson2.JSON;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.service.ISfStaskClockLocationService;
import com.ym.agriculture.api.farmtask.RemoteFarmTaskService;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskAcceptanceBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskClockInBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskCompleteBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteTaskLeaderAcceptBo;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteTaskViewVo;
import com.ym.agriculture.shared.dubbo.support.RemoteCommandIdempotencyExecutor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskAcceptanceBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskClockInBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskCompleteBo;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskLeaderAcceptBo;
import com.ym.agriculture.farmtask.workorder.service.ISfStaskWorkOrderService;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

/** 农事任务契约 Provider。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteFarmTaskServiceImpl implements RemoteFarmTaskService {

    private final ISfStaskClockLocationService clockLocationService;
    private final ISfStaskWorkOrderService workOrderService;
    private final RemoteCommandIdempotencyExecutor idempotencyExecutor;

    @Override
    public RemoteClockLocationVo getClockLocation() {
        SfStaskClockLocationVo source = clockLocationService.getCurrent();
        RemoteClockLocationVo target = new RemoteClockLocationVo();
        if (source == null) {
            target.setCoordinateType("CGCS2000");
            target.setMapProvider("tianditu");
            target.setConfigured(false);
            target.setFenceCheckRequired(false);
            return target;
        }
        target.setClockLocationId(source.getClockLocationId());
        target.setLocationName(source.getLocationName());
        target.setCenterLng(source.getCenterLng());
        target.setCenterLat(source.getCenterLat());
        target.setCoordinateType(source.getCoordinateType());
        target.setRadiusMeters(source.getRadiusMeters());
        target.setEnabled(source.getEnabled());
        target.setMapProvider(source.getMapProvider());
        target.setMapZoomLevel(source.getMapZoomLevel());
        target.setAddressText(source.getAddressText());
        target.setRemark(source.getRemark());
        target.setUpdateTime(source.getUpdateTime());
        target.setConfigured(source.getConfigured());
        target.setFenceCheckRequired(source.getFenceCheckRequired());
        return target;
    }

    @Override
    public RemoteTaskViewVo workbench(String roleCode) {
        return view(workOrderService.workbench(roleCode));
    }

    @Override
    public RemoteTaskViewVo workbenchSummary(String roleCode) {
        Object source = switch (roleCode) {
            case EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN -> workOrderService.managerWorkbenchSummary();
            case EmployeeConstants.APP_ROLE_STASK_EXPERT -> workOrderService.technicianWorkbenchSummary();
            case EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER -> workOrderService.leaderWorkbenchSummary();
            default -> workOrderService.workbench(roleCode);
        };
        return view(source);
    }

    @Override
    public RemoteTaskViewVo workbenchTasks(String roleCode, String tab) {
        Object source = switch (roleCode) {
            case EmployeeConstants.APP_ROLE_STASK_PRODUCTION_ADMIN -> workOrderService.managerWorkbenchTasks(tab);
            case EmployeeConstants.APP_ROLE_STASK_EXPERT -> workOrderService.technicianWorkbenchTasks(tab);
            case EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER -> workOrderService.leaderWorkbenchTasks(tab);
            default -> workOrderService.workbench(roleCode);
        };
        return view(source);
    }

    @Override
    public RemoteTaskViewVo detail(Long orderId) {
        return view(workOrderService.detail(orderId));
    }

    @Override
    public int leaderAccept(Long orderId, RemoteTaskLeaderAcceptBo command) {
        SfStaskLeaderAcceptBo bo = new SfStaskLeaderAcceptBo();
        bo.setRequiredWorkerCount(command.getRequiredWorkerCount());
        bo.setDailyLaborCount(command.getDailyLaborCount());
        bo.setLaborRecordVersion(command.getLaborRecordVersion());
        bo.setIdempotencyKey(command.getRequestId());
        return idempotencyExecutor.execute("TASK_LEADER_ACCEPT", command.getRequestId(), command.getBusinessId(),
            () -> workOrderService.leaderAccept(orderId, bo));
    }

    @Override
    public int clockIn(Long orderId, RemoteTaskClockInBo command) {
        SfStaskClockInBo bo = new SfStaskClockInBo();
        bo.setClockType(command.getClockType());
        bo.setLongitude(command.getLongitude());
        bo.setLatitude(command.getLatitude());
        bo.setProofPhotos(command.getProofPhotos());
        return idempotencyExecutor.execute("TASK_CLOCK_IN", command.getRequestId(), command.getBusinessId(),
            () -> workOrderService.clockIn(orderId, bo));
    }

    @Override
    public int complete(Long orderId, RemoteTaskCompleteBo command) {
        SfStaskCompleteBo bo = completeBo(command);
        return idempotencyExecutor.execute("TASK_COMPLETE", command.getRequestId(), command.getBusinessId(),
            () -> workOrderService.complete(orderId, bo));
    }

    @Override
    public int reapplyAcceptance(Long orderId, RemoteTaskCompleteBo command) {
        SfStaskCompleteBo bo = completeBo(command);
        return idempotencyExecutor.execute("TASK_REAPPLY_ACCEPTANCE", command.getRequestId(), command.getBusinessId(),
            () -> workOrderService.reapplyAcceptance(orderId, bo));
    }

    @Override
    public int acceptance(Long orderId, RemoteTaskAcceptanceBo command) {
        SfStaskAcceptanceBo bo = new SfStaskAcceptanceBo();
        bo.setResult(command.getResult());
        bo.setRejectReason(command.getRejectReason());
        bo.setAcceptancePhotos(command.getAcceptancePhotos());
        return idempotencyExecutor.execute("TASK_ACCEPTANCE", command.getRequestId(), command.getBusinessId(), () ->
            EmployeeConstants.APP_ROLE_STASK_EXPERT.equals(command.getAcceptorRoleCode())
                ? workOrderService.technicianAcceptance(orderId, bo)
                : workOrderService.acceptance(orderId, bo));
    }

    private static SfStaskCompleteBo completeBo(RemoteTaskCompleteBo command) {
        SfStaskCompleteBo bo = new SfStaskCompleteBo();
        bo.setWorkPhotos(command.getWorkPhotos());
        bo.setCompletionRemark(command.getCompletionRemark());
        return bo;
    }

    @SuppressWarnings("unchecked")
    private static RemoteTaskViewVo view(Object source) {
        if (source == null) {
            return new RemoteTaskViewVo();
        }
        LinkedHashMap<String, Object> fields = JSON.parseObject(JSON.toJSONString(source), LinkedHashMap.class);
        return new RemoteTaskViewVo(fields);
    }
}
