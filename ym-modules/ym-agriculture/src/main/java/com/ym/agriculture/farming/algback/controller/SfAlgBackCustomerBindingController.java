package com.ym.agriculture.farming.algback.controller;

import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farming.algback.model.bo.AlgBackCustomerNoBo;
import com.ym.agriculture.farming.algback.service.ISfAlgBackCustomerBindingManageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 租户级算法中台客户号绑定（与中台人工创建的客户号一致）。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/ai/alg-back/binding")
public class SfAlgBackCustomerBindingController extends BaseController {

    private final ISfAlgBackCustomerBindingManageService bindingManageService;

    /**
     * 更新当前租户的中台客户号
     *
     * @param bo 客户号请求体
     * @return 统一响应，成功无 {@code data} 体
     */
    @Log(title = "算法中台客户绑定", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @PutMapping("/customer-no")
    public R<Void> updateCustomerNo(@Valid @RequestBody AlgBackCustomerNoBo bo) {
        bindingManageService.updateCustomerNoForCurrentTenant(bo.getCustomerNo());
        return R.ok();
    }
}
