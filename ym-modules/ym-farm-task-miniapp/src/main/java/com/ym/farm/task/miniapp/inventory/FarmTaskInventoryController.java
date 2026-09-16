package com.ym.farm.task.miniapp.inventory;

import com.ym.agriculture.api.farmtask.RemoteInventoryService;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteInventoryCommandBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteInventoryQueryBo;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteInventoryViewVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 库存 V1.5、资产与领退料历史路径适配。
 *
 * <p>BFF 仅转换查询参数和幂等号，权限、数据范围与本地事务均由农业服务执行。</p>
 */
@RestController
@RequestMapping("/miniapp/smart-farming/stask")
public class FarmTaskInventoryController {

    @DubboReference
    private RemoteInventoryService inventoryService;

    @GetMapping("/inventory/workbench")
    public R<RemoteInventoryViewVo> workbench() {
        return R.ok(inventoryService.workbench());
    }

    @GetMapping("/inventory/items")
    public R<Map<String, Object>> items(@RequestParam Map<String, String> parameters) {
        return R.ok(miniPage(inventoryService.pageMaterials(query(parameters))));
    }

    @GetMapping("/inventory/items/{materialId}")
    public R<RemoteInventoryViewVo> item(@PathVariable Long materialId) {
        return R.ok(inventoryService.getMaterial(materialId));
    }

    @GetMapping("/inventory/items/{materialId}/ledger")
    public R<Map<String, Object>> ledger(@PathVariable Long materialId,
                                         @RequestParam Map<String, String> parameters) {
        return R.ok(miniPage(inventoryService.pageLedger(materialId, query(parameters))));
    }

    @GetMapping("/outbound-orders")
    public R<Map<String, Object>> outboundOrders(@RequestParam Map<String, String> parameters) {
        return R.ok(miniPage(inventoryService.pageOutbound(query(parameters))));
    }

    @GetMapping("/outbound-orders/{id}")
    public R<RemoteInventoryViewVo> outboundOrder(@PathVariable Long id) {
        return R.ok(inventoryService.getOutbound(id));
    }

    @PutMapping("/outbound-orders/{id}/pending-lines")
    public R<RemoteInventoryViewVo> savePendingLines(@PathVariable Long id,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.savePendingOutboundLines(id,
            command(requestId, "OUTBOUND_LINES:" + id, body)));
    }

    @PostMapping("/outbound-orders/{id}/confirm")
    public R<RemoteInventoryViewVo> confirmOutbound(@PathVariable Long id,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.confirmOutbound(id,
            command(requestId, "OUTBOUND_CONFIRM:" + id, body)));
    }

    @PostMapping("/outbound-orders/{id}/cancel")
    public R<RemoteInventoryViewVo> cancelOutbound(@PathVariable Long id,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.cancelOutbound(id,
            command(requestId, "OUTBOUND_CANCEL:" + id, body)));
    }

    @GetMapping("/material-receipts")
    public R<Map<String, Object>> receipts(@RequestParam Map<String, String> parameters) {
        return R.ok(miniPage(inventoryService.pageReceipts(query(parameters))));
    }

    @GetMapping("/material-receipts/{id}")
    public R<RemoteInventoryViewVo> receipt(@PathVariable Long id) {
        return R.ok(inventoryService.getReceipt(id));
    }

    @PostMapping("/material-receipts/{id}/apply-outbound")
    public R<RemoteInventoryViewVo> applyOutbound(@PathVariable Long id,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.applyOutbound(id,
            command(requestId, "RECEIPT_APPLY:" + id, body)));
    }

    @GetMapping("/return-orders")
    public R<Map<String, Object>> returns(@RequestParam Map<String, String> parameters) {
        return R.ok(miniPage(inventoryService.pageReturns(query(parameters))));
    }

    @GetMapping("/return-orders/{id}")
    public R<RemoteInventoryViewVo> returnDetail(@PathVariable Long id) {
        return R.ok(inventoryService.getReturn(id));
    }

    @PostMapping("/return-orders")
    public R<RemoteInventoryViewVo> createReturn(
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.createReturn(command(requestId, "RETURN_CREATE", body)));
    }

    @PutMapping("/return-orders/{id}")
    public R<RemoteInventoryViewVo> updateReturn(@PathVariable Long id,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.updateReturn(id, command(requestId, "RETURN_UPDATE:" + id, body)));
    }

    @DeleteMapping("/return-orders/{id}")
    public R<Void> deleteReturn(@PathVariable Long id, @RequestParam Long version,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        Map<String, Object> payload = Map.of("version", version);
        inventoryService.deleteReturn(id, version, command(requestId, "RETURN_DELETE:" + id, payload));
        return R.ok();
    }

    @PostMapping("/return-orders/{id}/confirm")
    public R<RemoteInventoryViewVo> confirmReturn(@PathVariable Long id,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        return R.ok(inventoryService.confirmReturn(id, command(requestId, "RETURN_CONFIRM:" + id, body)));
    }

    @GetMapping("/assets/devices")
    public R<Map<String, Object>> assets(@RequestParam Map<String, String> parameters) {
        return R.ok(miniPage(inventoryService.pageAssets(query(parameters))));
    }

    @GetMapping("/assets/devices/{id}")
    public R<RemoteInventoryViewVo> asset(@PathVariable Long id) {
        return R.ok(inventoryService.getAsset(id));
    }

    private static RemoteInventoryQueryBo query(Map<String, String> parameters) {
        Map<String, String> safe = parameters == null ? Map.of() : parameters;
        RemoteInventoryQueryBo query = new RemoteInventoryQueryBo();
        query.setPageNum(parsePositive(safe.get("pageNum"), 1));
        query.setPageSize(parsePositive(safe.get("pageSize"), 10));
        LinkedHashMap<String, Object> filters = new LinkedHashMap<>(safe);
        filters.remove("pageNum");
        filters.remove("pageSize");
        query.setFilters(filters);
        return query;
    }

    private static int parsePositive(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        int parsed = Integer.parseInt(value);
        return parsed > 0 ? parsed : fallback;
    }

    private static Map<String, Object> miniPage(PageResult<RemoteInventoryViewVo> page) {
        return Map.of("items", page.getRows(), "total", page.getTotal());
    }

    private static RemoteInventoryCommandBo command(String headerRequestId, String action,
                                                      Map<String, Object> body) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>(body == null ? Map.of() : body);
        String requestId = firstText(headerRequestId, payload.get("requestId"), payload.get("idempotencyKey"));
        if (requestId == null) {
            throw new IllegalArgumentException("写操作必须提供 X-Request-Id 或 idempotencyKey");
        }
        Object version = payload.get("version");
        Object sourceId = payload.get("materialReceiptId");
        String businessId = action + ":" + (sourceId == null ? "-" : sourceId) + ":"
            + (version == null ? "-" : version);
        RemoteInventoryCommandBo command = new RemoteInventoryCommandBo();
        command.setRequestId(requestId);
        command.setBusinessId(businessId);
        command.setPayload(payload);
        return command;
    }

    private static String firstText(String header, Object... candidates) {
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        for (Object candidate : candidates) {
            if (candidate != null && !candidate.toString().isBlank()) {
                return candidate.toString().trim();
            }
        }
        return null;
    }
}
