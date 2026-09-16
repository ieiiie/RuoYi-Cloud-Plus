package com.ym.agriculture.farming.algback.service.impl;

import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.algback.dao.SfAlgBackCustomerBindingMapper;
import com.ym.agriculture.farming.algback.model.entity.SfAlgBackCustomerBinding;
import com.ym.agriculture.farming.algback.service.ISfAlgBackCustomerBindingManageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 回填 {@code customer_no} 并将状态置为 {@link SfAlgBackCustomerBinding#STATUS_ACTIVE}。
 */
@RequiredArgsConstructor
@Service
public class SfAlgBackCustomerBindingManageServiceImpl implements ISfAlgBackCustomerBindingManageService {

    private final SfAlgBackCustomerBindingMapper bindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCustomerNoForCurrentTenant(String customerNo) {
        if (StringUtils.isBlank(customerNo)) {
            throw new ServiceException("customerNo 不能为空");
        }
        String tenantId = LoginHelper.getLoginUser().getTenantId();
        String cno = customerNo.trim();
        Long dup = bindingMapper.countByCustomerNoOtherTenants(cno, tenantId);
        if (dup != null && dup > 0) {
            throw new ServiceException("该中台客户号已被其他租户使用，一个客户号只能绑定一个租户");
        }
        SfAlgBackCustomerBinding row = bindingMapper.selectOneNormalByTenantId(tenantId);
        if (row == null) {
            throw new ServiceException("未找到本租户的算法中台绑定记录，请联系管理员检查租户初始化");
        }
        row.setCustomerNo(cno);
        row.setStatus(SfAlgBackCustomerBinding.STATUS_ACTIVE);
        bindingMapper.updateById(row);
    }
}
