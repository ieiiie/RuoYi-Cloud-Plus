package com.ym.agriculture.farming.algback.service;

/**
 * 维护当前租户与中台客户号绑定（人工在中台建客户后回填）。
 */
public interface ISfAlgBackCustomerBindingManageService {

    void updateCustomerNoForCurrentTenant(String customerNo);
}
