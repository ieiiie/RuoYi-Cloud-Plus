package org.dromara.common.tenant.filter;

import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.RpcServiceContext;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.tenant.helper.TenantHelper;

/**
 * Dubbo 租户上下文透传。
 *
 * <p>HTTP 请求由 Sa-Token 会话提供租户编号；服务间调用则需要把该编号
 * 放入 Dubbo Attachment。Provider 端只在本次调用作用域内恢复它，避免
 * 线程复用或嵌套 RPC 调用污染其他请求。</p>
 *
 * @author Lion Li
 */
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER}, order = -10_000)
public class TenantDubboFilter implements Filter {

    /**
     * Dubbo Attachment 中的租户编号键。
     */
    public static final String TENANT_ID_KEY = "tenant:id";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        RpcServiceContext context = RpcContext.getServiceContext();
        if (context.isConsumerSide()) {
            String tenantId = TenantHelper.getTenantId();
            if (StringUtils.isNotBlank(tenantId)) {
                context.setObjectAttachment(TENANT_ID_KEY, tenantId);
            }
            return invoker.invoke(invocation);
        }

        Object attachment = context.getObjectAttachment(TENANT_ID_KEY);
        String tenantId = attachment == null ? null : attachment.toString();
        if (!TenantHelper.isEnable() || StringUtils.isBlank(tenantId)) {
            return invoker.invoke(invocation);
        }
        return TenantHelper.dynamic(tenantId, () -> invoker.invoke(invocation));
    }

}
