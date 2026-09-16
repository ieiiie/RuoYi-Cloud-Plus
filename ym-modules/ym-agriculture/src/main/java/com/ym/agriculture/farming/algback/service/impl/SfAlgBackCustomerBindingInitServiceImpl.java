package com.ym.agriculture.farming.algback.service.impl;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.algback.dao.SfAlgBackCustomerBindingMapper;
import com.ym.agriculture.farming.algback.model.entity.SfAlgBackCustomerBinding;
import com.ym.agriculture.farming.algback.service.ISfAlgBackCustomerBindingInitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 新建租户后插入 {@code PENDING} 绑定行（每租户一行）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SfAlgBackCustomerBindingInitServiceImpl implements ISfAlgBackCustomerBindingInitService {

    private final SfAlgBackCustomerBindingMapper bindingMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensurePendingBindingForTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            return;
        }
        String tid = tenantId.trim();
        long n = bindingMapper.countNormalByTenantId(tid);
        if (n > 0) {
            return;
        }
        SfAlgBackCustomerBinding row = new SfAlgBackCustomerBinding();
        row.setTenantId(tid);
        row.setStatus(SfAlgBackCustomerBinding.STATUS_PENDING);
        row.setCustomerNo(null);
        bindingMapper.insert(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyCustomerNoFromMidPlatform(String tenantId, String customerNo) {
        if (StringUtils.isBlank(tenantId) || StringUtils.isBlank(customerNo)) {
            return;
        }
        String tid = tenantId.trim();
        String cno = customerNo.trim();
        Long dup = bindingMapper.countByCustomerNoOtherTenants(cno, tid);
        if (dup != null && dup > 0) {
            log.warn("中台客户号 {} 已被其他租户占用，拒绝写入本租户 {}", cno, tid);
            return;
        }
        SfAlgBackCustomerBinding row = bindingMapper.selectOneNormalByTenantId(tid);
        if (row == null) {
            SfAlgBackCustomerBinding insert = new SfAlgBackCustomerBinding();
            insert.setTenantId(tid);
            insert.setCustomerNo(cno);
            insert.setStatus(SfAlgBackCustomerBinding.STATUS_ACTIVE);
            bindingMapper.insert(insert);
            return;
        }
        row.setCustomerNo(cno);
        row.setStatus(SfAlgBackCustomerBinding.STATUS_ACTIVE);
        bindingMapper.updateById(row);
    }
}
