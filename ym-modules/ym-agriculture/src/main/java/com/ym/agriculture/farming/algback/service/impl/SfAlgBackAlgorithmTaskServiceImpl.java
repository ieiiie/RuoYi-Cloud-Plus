package com.ym.agriculture.farming.algback.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackClientException;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackSync;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAddTaskResult;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAlgorithmTaskAddRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.task.AlgBackAlgorithmTaskStatusRequest;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.algback.dao.SfAlgBackCustomerBindingMapper;
import com.ym.agriculture.farming.algback.model.bo.AlgBackTaskSubmitBo;
import com.ym.agriculture.farming.algback.model.bo.AlgBackTaskStatusBo;
import com.ym.agriculture.farming.algback.model.entity.SfAlgBackCustomerBinding;
import com.ym.agriculture.farming.algback.service.ISfAlgBackAlgorithmTaskService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.annotation.Transactional;


/**
 * 按当前租户绑定中的 {@code customerNo} 调中台；未配置 {@code ym.alg-back} 时拒绝。
 */
@RequiredArgsConstructor
@Service
public class SfAlgBackAlgorithmTaskServiceImpl implements ISfAlgBackAlgorithmTaskService {

    private final ObjectProvider<AlgBackApi> algBackApiProvider;
    private final SfAlgBackCustomerBindingMapper bindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public AlgBackAddTaskResult submitAndOptionallyStart(AlgBackTaskSubmitBo bo) {
        AlgBackApi api = algBackApiProvider.getIfAvailable();
        if (api == null) {
            throw new ServiceException("未配置算法中台，请在应用中配置 ym.alg-back.base-url 并完成登录");
        }
        String customerNo = resolveCustomerNoForCurrentTenant();
        AlgBackAlgorithmTaskAddRequest req = AlgBackAlgorithmTaskAddRequest.builder()
            .modelNo(bo.getModelNo().trim())
            .customerNo(customerNo)
            .videoPlayUrl(bo.getVideoPlayUrl().trim())
            .taskName(bo.getTaskName())
            .skipFrame(bo.getSkipFrame())
            .pushFrequency(bo.getPushFrequency())
            .confThreshold(bo.getConfThreshold())
            .nmsThreshold(bo.getNmsThreshold())
            .videoBaseInfo(bo.getVideoBaseInfo())
            .build();
        try {
            AlgBackAddTaskResult created = AlgBackSync.execute(api.addAlgorithmTask(req));
            if (bo.isStartAfterCreate() && created != null && StringUtils.isNotBlank(created.getTaskNo())) {
                AlgBackSync.execute(api.setAlgorithmTaskStatus(
                    new AlgBackAlgorithmTaskStatusRequest(created.getTaskNo(), (byte) 1)));
            }
            return created;
        } catch (AlgBackClientException e) {
            throw new ServiceException("调用算法中台失败: " + e.getMessage());
        }
    }


    @Override
    @Transactional(rollbackFor = Exception.class, readOnly = true)
    public void setTaskStatus(AlgBackTaskStatusBo bo) {
        AlgBackApi api = algBackApiProvider.getIfAvailable();
        if (api == null) {
            throw new ServiceException("未配置算法中台，请在应用中配置 ym.alg-back.base-url 并完成登录");
        }
        try {
            AlgBackSync.execute(api.setAlgorithmTaskStatus(
                new AlgBackAlgorithmTaskStatusRequest(bo.getTaskNo().trim(), bo.getTaskStatus())));
        } catch (AlgBackClientException e) {
            throw new ServiceException("调用算法中台失败: " + e.getMessage());
        }
    }

    private String resolveCustomerNoForCurrentTenant() {
        String tenantId = LoginHelper.getLoginUser().getTenantId();
        SfAlgBackCustomerBinding row = bindingMapper.selectOneNormalByTenantId(tenantId);
        if (row == null || StringUtils.isBlank(row.getCustomerNo())) {
            throw new ServiceException("本租户未配置算法中台客户号，请先调用绑定接口回填 customerNo（与中台一致）");
        }
        return row.getCustomerNo().trim();
    }
}
