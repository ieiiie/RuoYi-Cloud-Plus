package com.ym.agriculture.farming.algback.model.bo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 当前租户在算法中台的客户号绑定。
 *
 * @author ym-cloud
 */
@Data
public class AlgBackCustomerNoBo {

    /**
     * 中台侧客户编号（人工开户后回填）
     */
    @NotBlank
    private String customerNo;
}
