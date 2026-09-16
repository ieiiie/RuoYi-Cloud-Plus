package com.ym.agriculture.farmtask.dubbo;

import com.alibaba.fastjson2.JSON;
import com.ym.agriculture.api.farmtask.RemoteInventoryService;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteInventoryCommandBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteInventoryQueryBo;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteInventoryViewVo;
import com.ym.agriculture.shared.dubbo.support.RemoteCommandIdempotencyExecutor;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.inventory.InventoryBusinessException;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.CancelOutboundBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.LedgerQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OrderQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.PendingLinesSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptApplyBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnCreateBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnUpdateBo;
import com.ym.agriculture.farmtask.inventory.service.IAssetInventoryService;
import com.ym.agriculture.farmtask.inventory.service.IInventoryService;
import com.ym.agriculture.farmtask.inventory.service.IMaterialReceiptReturnService;
import com.ym.agriculture.farmtask.inventory.support.MiniappInventoryScopeResolver;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.satoken.utils.LoginHelper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/** 库存 V1.5 和领退料契约 Provider。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteInventoryServiceImpl implements RemoteInventoryService {

    private final IInventoryService inventoryService;
    private final IMaterialReceiptReturnService receiptReturnService;
    private final IAssetInventoryService assetService;
    private final MiniappInventoryScopeResolver scopeResolver;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final RemoteCommandIdempotencyExecutor idempotencyExecutor;

    @Override
    public RemoteInventoryViewVo workbench() {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        RemoteInventoryViewVo result = view(inventoryService.workbench());
        result.put("pendingReturnCount", receiptReturnService.pendingReturnCount());
        result.put("assetCount", assetService.assetCount());
        return result;
    }

    @Override
    public PageResult<RemoteInventoryViewVo> pageMaterials(RemoteInventoryQueryBo query) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return page(inventoryService.pageMaterials(convert(query, MaterialQuery.class), pageQuery(query)));
    }

    @Override
    public RemoteInventoryViewVo getMaterial(Long materialId) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return view(inventoryService.getMaterial(materialId));
    }

    @Override
    public PageResult<RemoteInventoryViewVo> pageLedger(Long materialId, RemoteInventoryQueryBo query) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return page(inventoryService.pageLedger(materialId, convert(query, LedgerQuery.class), pageQuery(query)));
    }

    @Override
    public PageResult<RemoteInventoryViewVo> pageOutbound(RemoteInventoryQueryBo query) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return page(inventoryService.pageOutbound(convert(query, OrderQuery.class), pageQuery(query)));
    }

    @Override
    public RemoteInventoryViewVo getOutbound(Long outboundId) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return view(inventoryService.getOutbound(outboundId));
    }

    @Override
    public RemoteInventoryViewVo savePendingOutboundLines(Long outboundId, RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        PendingLinesSaveBo bo = payload(command, PendingLinesSaveBo.class);
        return mutate("INVENTORY_OUTBOUND_LINES", command,
            () -> inventoryService.savePendingOutboundLines(outboundId, bo));
    }

    @Override
    public RemoteInventoryViewVo confirmOutbound(Long outboundId, RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        OutboundConfirmBo bo = payload(command, OutboundConfirmBo.class);
        bo.setIdempotencyKey(command.getRequestId());
        return mutate("INVENTORY_OUTBOUND_CONFIRM", command,
            () -> inventoryService.confirmOutbound(outboundId, bo));
    }

    @Override
    public RemoteInventoryViewVo cancelOutbound(Long outboundId, RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return mutate("INVENTORY_OUTBOUND_CANCEL", command,
            () -> inventoryService.cancelOutbound(outboundId, payload(command, CancelOutboundBo.class)));
    }

    @Override
    public PageResult<RemoteInventoryViewVo> pageReceipts(RemoteInventoryQueryBo query) {
        String role = scopeResolver.roleCode();
        return page(receiptReturnService.pageReceipts(convert(query, OrderQuery.class), pageQuery(query), role,
            scopeResolver.employeeId()));
    }

    @Override
    public RemoteInventoryViewVo getReceipt(Long receiptId) {
        return view(receiptReturnService.getReceipt(receiptId, scopeResolver.receiptDetailRoleCode(),
            scopeResolver.employeeId()));
    }

    @Override
    public RemoteInventoryViewVo applyOutbound(Long receiptId, RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
        ReceiptApplyBo bo = payload(command, ReceiptApplyBo.class);
        bo.setIdempotencyKey(command.getRequestId());
        return mutate("INVENTORY_RECEIPT_APPLY", command,
            () -> receiptReturnService.applyOutbound(receiptId, bo, scopeResolver.employeeId()));
    }

    @Override
    public PageResult<RemoteInventoryViewVo> pageReturns(RemoteInventoryQueryBo query) {
        String role = scopeResolver.roleCode();
        return page(receiptReturnService.pageReturns(convert(query, OrderQuery.class), pageQuery(query), role,
            scopeResolver.employeeId()));
    }

    @Override
    public RemoteInventoryViewVo getReturn(Long returnId) {
        String role = scopeResolver.roleCode();
        return view(receiptReturnService.getReturn(returnId, role, scopeResolver.employeeId()));
    }

    @Override
    public RemoteInventoryViewVo createReturn(RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
        ReturnCreateBo bo = payload(command, ReturnCreateBo.class);
        bo.setIdempotencyKey(command.getRequestId());
        return mutate("INVENTORY_RETURN_CREATE", command,
            () -> receiptReturnService.createReturn(bo, scopeResolver.employeeId()));
    }

    @Override
    public RemoteInventoryViewVo updateReturn(Long returnId, RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER);
        return mutate("INVENTORY_RETURN_UPDATE", command,
            () -> receiptReturnService.updateReturn(returnId, payload(command, ReturnUpdateBo.class),
                scopeResolver.employeeId()));
    }

    @Override
    public void deleteReturn(Long returnId, Long version, RemoteInventoryCommandBo command) {
        requireAnyRole(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER,
            EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        idempotencyExecutor.execute("INVENTORY_RETURN_DELETE", command.getRequestId(), command.getBusinessId(), () -> {
            receiptReturnService.deleteReturn(returnId, version, scopeResolver.employeeId(), scopeResolver.isKeeper());
            return 1;
        });
    }

    @Override
    public RemoteInventoryViewVo confirmReturn(Long returnId, RemoteInventoryCommandBo command) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        ReturnConfirmBo bo = payload(command, ReturnConfirmBo.class);
        bo.setIdempotencyKey(command.getRequestId());
        return mutate("INVENTORY_RETURN_CONFIRM", command,
            () -> receiptReturnService.confirmReturn(returnId, bo));
    }

    @Override
    public PageResult<RemoteInventoryViewVo> pageAssets(RemoteInventoryQueryBo query) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return page(assetService.pageDevices(convert(query, AssetQuery.class), pageQuery(query)));
    }

    @Override
    public RemoteInventoryViewVo getAsset(Long assetId) {
        requireRole(EmployeeConstants.APP_ROLE_STASK_WAREHOUSE_KEEPER);
        return view(assetService.getDevice(assetId));
    }

    private RemoteInventoryViewVo mutate(String action, RemoteInventoryCommandBo command, Supplier<?> operation) {
        AtomicReference<Object> result = new AtomicReference<>();
        idempotencyExecutor.execute(action, command.getRequestId(), command.getBusinessId(), () -> {
            result.set(operation.get());
            return 1;
        });
        if (result.get() != null) {
            return view(result.get());
        }
        RemoteInventoryViewVo replay = new RemoteInventoryViewVo();
        replay.put("businessId", command.getBusinessId());
        replay.put("requestId", command.getRequestId());
        replay.put("idempotentReplay", true);
        return replay;
    }

    private void requireRole(String role) {
        Long employeeId = LoginHelper.getUserId();
        if (employeeId == null || !employeeAccessor.hasAppRole(employeeId, role)) {
            forbidden();
        }
    }

    private void requireAnyRole(String... roles) {
        Long employeeId = LoginHelper.getUserId();
        if (employeeId != null) {
            for (String role : roles) {
                if (employeeAccessor.hasAppRole(employeeId, role)) {
                    return;
                }
            }
        }
        forbidden();
    }

    private static void forbidden() {
        throw new InventoryBusinessException(InventoryConstants.ERROR_ACTION_FORBIDDEN, 403,
            "当前岗位无库存功能权限");
    }

    private static PageQuery pageQuery(RemoteInventoryQueryBo query) {
        RemoteInventoryQueryBo safe = query == null ? new RemoteInventoryQueryBo() : query;
        return new PageQuery(safe.getPageSize(), safe.getPageNum());
    }

    private static <T> T convert(RemoteInventoryQueryBo query, Class<T> type) {
        Object filters = query == null ? null : query.getFilters();
        return JSON.parseObject(JSON.toJSONString(filters == null ? new LinkedHashMap<>() : filters), type);
    }

    private static <T> T payload(RemoteInventoryCommandBo command, Class<T> type) {
        return JSON.parseObject(JSON.toJSONString(command.getPayload()), type);
    }

    private static PageResult<RemoteInventoryViewVo> page(PageResult<?> source) {
        return PageResult.build(source.getRows().stream().map(RemoteInventoryServiceImpl::view).toList(),
            source.getTotal());
    }

    @SuppressWarnings("unchecked")
    private static RemoteInventoryViewVo view(Object source) {
        if (source == null) {
            return new RemoteInventoryViewVo();
        }
        LinkedHashMap<String, Object> fields = JSON.parseObject(JSON.toJSONString(source), LinkedHashMap.class);
        return new RemoteInventoryViewVo(fields);
    }
}
