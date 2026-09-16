package com.ym.agriculture.farming.algback.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.ym.agriculture.farming.integration.ai.algback.api.AlgBackApi;
import com.ym.agriculture.farming.integration.ai.algback.autoconfigure.YmAlgBackProperties;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackClientException;
import com.ym.agriculture.farming.integration.ai.algback.client.AlgBackSync;
import com.ym.agriculture.farming.integration.ai.algback.dto.common.AlgBackPageVo;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackCustomerAddRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackCustomerPageRequest;
import com.ym.agriculture.farming.integration.ai.algback.dto.customer.AlgBackCustomerUpdateRequest;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.json.utils.JsonUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.algback.dao.SfAlgBackCustomerBindingMapper;
import com.ym.agriculture.farming.algback.model.entity.SfAlgBackCustomerBinding;
import com.ym.agriculture.farming.algback.service.ISfAlgBackCustomerBindingInitService;
import com.ym.agriculture.farming.algback.service.ISfAlgBackTenantProvisionService;
import com.ym.agriculture.api.farming.domain.bo.RemoteTenantInitializationBo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * 调用中台：客户名称 = {@code 租户id-企业名称} 与租户唯一对应；保证<strong>一租户一中台客户</strong>（本地唯一 + 幂等查中台）。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SfAlgBackTenantProvisionServiceImpl implements ISfAlgBackTenantProvisionService {

    private static final int LIST_PAGE_SIZE = 50;
    private static final int RESOLVE_RETRY = 4;
    private static final long RESOLVE_RETRY_DELAY_MS = 300L;
    private static final int EXISTING_MID_FETCH_RETRY = 2;

    private static final int MAX_MID_CUSTOMER_NAME_LEN = 200;

    private final ObjectProvider<AlgBackApi> algBackApiProvider;
    private final YmAlgBackProperties algBackProperties;
    private final ISfAlgBackCustomerBindingInitService bindingInitService;
    private final SfAlgBackCustomerBindingMapper bindingMapper;

    @Override
    public void provisionMidCustomerIfEnabled(RemoteTenantInitializationBo event) {
        if (event == null || !algBackProperties.isAutoCreateCustomer()) {
            return;
        }
        AlgBackApi api = algBackApiProvider.getIfAvailable();
        if (api == null) {
            log.warn("租户 {} 跳过算法中台自动建客户：未配置 ym.alg-back.base-url", event.getTenantId());
            return;
        }
        String callbackUrl = algBackProperties.getCallbackUrl();
        if (StringUtils.isBlank(callbackUrl)) {
            log.warn("租户 {} 跳过算法中台自动建客户：未配置 ym.alg-back.callback-url", event.getTenantId());
            return;
        }
        String companyName = event.getCompanyName();
        String mobile = normalizeMobile(event.getContactPhone());
        if (StringUtils.isBlank(companyName) || StringUtils.isBlank(mobile)) {
            log.warn("租户 {} 跳过算法中台自动建客户：企业名称或联系电话为空", event.getTenantId());
            return;
        }
        String tid = event.getTenantId().trim();
        String midCustomerName = buildMidCustomerName(tid, companyName);
        String urlTrim = callbackUrl.trim();

        if (hasActiveCustomerBinding(tid)) {
            log.info("租户 {} 已存在算法中台客户绑定（ACTIVE），跳过自动建客户", tid);
            return;
        }

        JsonNode existingOnMid = fetchMidCustomerByNameWithRetry(api, midCustomerName);
        if (existingOnMid != null && !existingOnMid.isNull()) {
            log.info("租户 {} 中台已存在客户名称 [{}]，执行启用与本地同步", tid, midCustomerName);
            enableAndSyncBinding(tid, existingOnMid, midCustomerName, mobile, urlTrim, api);
            return;
        }

        try {
            HashMap<String,String> hashMap = new HashMap<>();
            hashMap.put("tenantId",tid);
            AlgBackSync.execute(api.addCustomer(AlgBackCustomerAddRequest.builder()
                .name(midCustomerName)
                .mobileNum(mobile)
                .httpReqUrl(urlTrim)
                    .httpReqHeader(JsonUtils.toJsonString(hashMap))
                .build()));
        } catch (AlgBackClientException e) {
            log.warn("租户 {} 算法中台 addCustomer 失败: {}", tid, e.getMessage());
            return;
        }
        JsonNode row = resolveCustomerRowAfterCreate(api, midCustomerName, mobile);
        if (row == null || row.isNull()) {
            log.warn("租户 {} 算法中台建客户后未在列表中解析到记录 midName={}", tid, midCustomerName);
            return;
        }
        enableAndSyncBinding(tid, row, midCustomerName, mobile, urlTrim, api);
    }

    private boolean hasActiveCustomerBinding(String tenantId) {
        SfAlgBackCustomerBinding b = bindingMapper.selectByTenantIdIgnoreTenant(tenantId);
        return b != null
            && StringUtils.isNotBlank(b.getCustomerNo())
            && SfAlgBackCustomerBinding.STATUS_ACTIVE.equals(b.getStatus());
    }

    private JsonNode fetchMidCustomerByNameWithRetry(AlgBackApi api, String midCustomerName) {
        for (int i = 0; i < EXISTING_MID_FETCH_RETRY; i++) {
            JsonNode n = fetchMidCustomerByNameOnce(api, midCustomerName);
            if (n != null) {
                return n;
            }
            if (i + 1 < EXISTING_MID_FETCH_RETRY) {
                try {
                    Thread.sleep(RESOLVE_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return null;
    }

    private JsonNode fetchMidCustomerByNameOnce(AlgBackApi api, String midCustomerName) {
        try {
            AlgBackPageVo page = AlgBackSync.execute(api.getCustomerListPageVo(
                AlgBackCustomerPageRequest.builder()
                    .pageNum(1)
                    .pageSize(LIST_PAGE_SIZE)
                    .searchKey(midCustomerName)
                    .build()));
            return findRowByMidCustomerName(page, midCustomerName);
        } catch (AlgBackClientException e) {
            log.debug("查询中台客户（按名称）失败: {}", e.getMessage());
            return null;
        }
    }

    private void enableAndSyncBinding(String tenantId, JsonNode row, String midCustomerName,
                                      String mobile, String urlTrim, AlgBackApi api) {
        Long customerId = readCustomerId(row);
        String customerNo = readText(row, "customerNo");
        if (customerId == null || StringUtils.isBlank(customerNo)) {
            log.warn("租户 {} 算法中台客户记录缺少 id 或 customerNo", tenantId);
            return;
        }
        Long other = bindingMapper.countByCustomerNoOtherTenants(customerNo.trim(), tenantId);
        if (other != null && other > 0) {
            log.warn("租户 {} 无法绑定：中台客户号 {} 已被其他租户占用（一客户号仅对应一租户）", tenantId, customerNo);
            return;
        }
        try {
            AlgBackSync.execute(api.updateCustomer(AlgBackCustomerUpdateRequest.builder()
                .id(customerId)
                .name(midCustomerName)
                .mobileNum(mobile)
                .status((byte) 1)
                .httpReqUrl(urlTrim)
                .build()));
        } catch (AlgBackClientException e) {
            log.warn("租户 {} 算法中台 updateCustomer 启用失败: {}", tenantId, e.getMessage());
            return;
        }
        try {
            TenantHelper.dynamic(tenantId, () ->
                bindingInitService.applyCustomerNoFromMidPlatform(tenantId, customerNo.trim()));
        } catch (Exception e) {
            log.warn("租户 {} 回写本地算法中台绑定失败 customerNo={}: {}", tenantId, customerNo, e.getMessage());
            return;
        }
        log.info("租户 {} 算法中台客户已就绪 customerNo={} midName={}", tenantId, customerNo, midCustomerName);
    }

    private static String buildMidCustomerName(String tenantId, String companyName) {
        String tid = tenantId == null ? "" : tenantId.trim();
        String cn = companyName == null ? "" : companyName.trim().replaceAll("\\s+", " ");
        String s = tid + "-" + cn;
        if (s.length() > MAX_MID_CUSTOMER_NAME_LEN) {
            return s.substring(0, MAX_MID_CUSTOMER_NAME_LEN);
        }
        return s;
    }

    private static JsonNode resolveCustomerRowAfterCreate(AlgBackApi api, String midCustomerName, String mobile) {
        AlgBackCustomerPageRequest req = AlgBackCustomerPageRequest.builder()
            .pageNum(1)
            .pageSize(LIST_PAGE_SIZE)
            .searchKey(midCustomerName)
            .build();
        for (int attempt = 0; attempt < RESOLVE_RETRY; attempt++) {
            if (attempt > 0) {
                try {
                    Thread.sleep(RESOLVE_RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            try {
                AlgBackPageVo page = AlgBackSync.execute(api.getCustomerListPageVo(req));
                JsonNode hit = findRowByMidCustomerName(page, midCustomerName);
                if (hit != null) {
                    return hit;
                }
                hit = findRowByMobile(page, mobile);
                if (hit != null) {
                    return hit;
                }
            } catch (AlgBackClientException e) {
                log.debug("解析中台客户列表失败 attempt={}: {}", attempt, e.getMessage());
            }
        }
        return null;
    }

    private static JsonNode findRowByMidCustomerName(AlgBackPageVo page, String midCustomerName) {
        if (page == null || page.getList() == null || StringUtils.isBlank(midCustomerName)) {
            return null;
        }
        for (JsonNode n : page.getList()) {
            if (n == null || n.isNull()) {
                continue;
            }
            String nm = readText(n, "name");
            if (midCustomerName.equals(nm)) {
                return n;
            }
        }
        return null;
    }

    private static JsonNode findRowByMobile(AlgBackPageVo page, String mobile) {
        if (page == null || page.getList() == null) {
            return null;
        }
        for (JsonNode n : page.getList()) {
            if (n == null || n.isNull()) {
                continue;
            }
            String mn = readText(n, "mobileNum");
            if (mobile.equals(mn)) {
                return n;
            }
        }
        return null;
    }

    private static String readText(JsonNode n, String field) {
        if (n == null || !n.has(field) || n.get(field).isNull()) {
            return "";
        }
        return n.get(field).asText("").trim();
    }

    private static Long readCustomerId(JsonNode row) {
        if (!row.has("id") || row.get("id").isNull()) {
            return null;
        }
        JsonNode idNode = row.get("id");
        if (idNode.isNumber()) {
            return idNode.longValue();
        }
        String t = idNode.asText();
        if (StringUtils.isBlank(t)) {
            return null;
        }
        try {
            return Long.parseLong(t.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalizeMobile(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().replaceAll("[\\s-]+", "");
    }
}
