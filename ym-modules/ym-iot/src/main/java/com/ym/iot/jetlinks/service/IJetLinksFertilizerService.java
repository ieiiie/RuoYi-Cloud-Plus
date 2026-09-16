package com.ym.iot.jetlinks.service;

import com.ym.iot.fertilizer.domain.bo.FertilizerResetBo;
import com.ym.iot.fertilizer.domain.bo.FertilizerTankParamBo;
import com.ym.iot.fertilizer.domain.dto.FertilizerRuntimeConfig;
import com.ym.iot.fertilizer.domain.vo.FertilizerStateSnapshot;
import com.ym.iot.fertilizer.domain.vo.TaskInfo;

import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksFertilizerService {
    FertilizerStateSnapshot getState(Long id);

    FertilizerStateSnapshot getStateWithParams(Long id);

    FertilizerRuntimeConfig runtimeConfig();

    TaskInfo start(Long id, List<FertilizerTankParamBo> ignoredCompatibilityParams);

    TaskInfo startWithoutReset(Long id);

    TaskInfo stop(Long id);

    TaskInfo emergencyStop(Long id, String reason);

    TaskInfo primeWater(Long id);

    TaskInfo cleanTank(Long id);

    TaskInfo reset(Long id);

    TaskInfo reset(Long id, FertilizerResetBo bo);

    TaskInfo refreshRealtime(Long id);

    TaskInfo readCurrentParams(Long id);

    TaskInfo applyParams(Long id, List<FertilizerTankParamBo> tanks);

    TaskInfo getTask(String id);

    TaskInfo getActiveTask(Long id);
}
