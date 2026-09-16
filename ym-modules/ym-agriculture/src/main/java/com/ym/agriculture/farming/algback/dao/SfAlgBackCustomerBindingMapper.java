package com.ym.agriculture.farming.algback.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.algback.model.entity.SfAlgBackCustomerBinding;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code sf_ai_customer_binding}。
 */
public interface SfAlgBackCustomerBindingMapper extends BaseMapper<SfAlgBackCustomerBinding> {

    /**
     * 回调无登录租户上下文：按中台客户号解析租户。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sf_ai_customer_binding WHERE del_flag = '0' AND customer_no = #{customerNo} LIMIT 1")
    SfAlgBackCustomerBinding selectByCustomerNoIgnoreTenant(@Param("customerNo") String customerNo);

    /** 无登录租户上下文时按 tenant_id 查绑定（如租户创建异步监听）。 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sf_ai_customer_binding WHERE del_flag = '0' AND tenant_id = #{tenantId} LIMIT 1")
    SfAlgBackCustomerBinding selectByTenantIdIgnoreTenant(@Param("tenantId") String tenantId);

    /**
     * 是否已有<strong>其他租户</strong>占用该中台客户号（保证一客户号只对应一租户）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM sf_ai_customer_binding WHERE del_flag = '0' AND customer_no = #{customerNo} AND tenant_id <> #{tenantId}")
    Long countByCustomerNoOtherTenants(@Param("customerNo") String customerNo, @Param("tenantId") String tenantId);

    default long countNormalByTenantId(String tenantId) {
        return selectCount(Wrappers.lambdaQuery(SfAlgBackCustomerBinding.class)
            .eq(SfAlgBackCustomerBinding::getTenantId, tenantId)
            .eq(SfAlgBackCustomerBinding::getDelFlag, "0"));
    }

    default SfAlgBackCustomerBinding selectOneNormalByTenantId(String tenantId) {
        return selectOne(Wrappers.lambdaQuery(SfAlgBackCustomerBinding.class)
            .eq(SfAlgBackCustomerBinding::getTenantId, tenantId)
            .eq(SfAlgBackCustomerBinding::getDelFlag, "0"));
    }
}
