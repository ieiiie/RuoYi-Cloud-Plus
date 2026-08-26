package org.dromara.common.core.constant;

/**
 * 多租户通用常量。
 *
 * <p>默认租户承载升级前已有的系统数据，也是平台管理员所在的租户。
 * 业务代码不应将该租户编号硬编码为任意其他值。</p>
 *
 * @author Lion Li
 */
public interface TenantConstants {

    /**
     * 升级后存量数据所属的默认租户。
     */
    String DEFAULT_TENANT_ID = "000000";

    /**
     * 租户管理员角色名称。
     */
    String TENANT_ADMIN_ROLE_NAME = "租户管理员";

    /**
     * 租户管理员角色标识。
     */
    String TENANT_ADMIN_ROLE_KEY = "tenant_admin";

}
