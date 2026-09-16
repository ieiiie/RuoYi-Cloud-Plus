package com.ym.agriculture.farmtask.inventory.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.ym.common.core.domain.R;
import com.ym.common.redis.annotation.RepeatSubmit;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.CorrectionDeleteBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.OutboundCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.OutboundCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnOperationLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DirectOutboundSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DirectReturnSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.CancelOutboundBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.InboundSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.InventorySummaryVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.LedgerQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.LedgerVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialCategorySaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialCategoryVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MaterialVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OrderQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.PendingLinesSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnCreateBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeDetailVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeVo;
import com.ym.agriculture.farmtask.inventory.service.IInventoryCorrectionService;
import com.ym.agriculture.farmtask.inventory.service.IInventoryService;
import com.ym.agriculture.farmtask.inventory.service.IMaterialReceiptReturnService;
import com.ym.agriculture.farmtask.inventory.support.InventorySummaryAssembler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/stask/inventory")
public class InventoryController extends BaseController {

    private final IInventoryService inventoryService;
    private final IInventoryCorrectionService correctionService;
    private final IMaterialReceiptReturnService returnService;
    private final InventorySummaryAssembler summaryAssembler;

    /**
     * 返回当前租户的权限化库存统计；无权限字段由响应模型省略。
     */
    @SaCheckPermission(value = {
        InventorySummaryAssembler.OUTBOUND_PERMISSION,
        InventorySummaryAssembler.RETURN_PERMISSION,
        InventorySummaryAssembler.BALANCE_PERMISSION,
        InventorySummaryAssembler.MATERIAL_PERMISSION,
        InventorySummaryAssembler.ASSET_PERMISSION
    }, mode = SaMode.OR)
    @GetMapping("/summary")
    public R<InventorySummaryVo> summary() {
        return R.ok(summaryAssembler.project(inventoryService.workbench(), StpUtil::hasPermission));
    }

    /**
     * 查询当前租户的全部物料分类及有效物料数量。
     */
    @SaCheckPermission("inventory:material:list")
    @GetMapping("/material-categories")
    public R<List<MaterialCategoryVo>> materialCategories() {
        return R.ok(inventoryService.listMaterialCategories());
    }

    /**
     * 新增当前租户的物料分类。
     */
    @Log(title = "物料分类", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:materialcategory:add")
    @PostMapping("/material-categories")
    public R<MutationVo> addMaterialCategory(@Valid @RequestBody MaterialCategorySaveBo bo) {
        return R.ok(inventoryService.addMaterialCategory(bo));
    }

    /**
     * 按版本更新当前租户的物料分类。
     */
    @Log(title = "物料分类", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:materialcategory:edit")
    @PutMapping("/material-categories/{id}")
    public R<MutationVo> updateMaterialCategory(@PathVariable Long id,
        @Valid @RequestBody MaterialCategorySaveBo bo) {
        return R.ok(inventoryService.updateMaterialCategory(id, bo));
    }

    /**
     * 删除当前租户中从未被物料引用的分类。
     */
    @Log(title = "物料分类", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @SaCheckPermission("inventory:materialcategory:remove")
    @DeleteMapping("/material-categories/{id}")
    public R<Void> removeMaterialCategory(@PathVariable Long id, @RequestParam Long version) {
        inventoryService.removeMaterialCategory(id, version);
        return R.ok();
    }

    @SaCheckPermission("inventory:material:list")
    @GetMapping("/materials")
    public R<PageResult<MaterialVo>> materials(MaterialQuery query, PageQuery pageQuery) {
        return R.ok(inventoryService.pageMaterials(query, pageQuery));
    }

    @SaCheckPermission("inventory:material:list")
    @GetMapping("/materials/{id}")
    public R<MaterialVo> material(@PathVariable Long id) {
        return R.ok(inventoryService.getMaterial(id));
    }

    @Log(title = "库存物资", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:material:add")
    @PostMapping("/materials")
    public R<MutationVo> addMaterial(@Valid @RequestBody MaterialSaveBo bo) {
        return R.ok(inventoryService.addMaterial(bo));
    }

    @Log(title = "库存物资", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:material:edit")
    @PutMapping("/materials/{id}")
    public R<MutationVo> updateMaterial(@PathVariable Long id, @Valid @RequestBody MaterialSaveBo bo) {
        return R.ok(inventoryService.updateMaterial(id, bo));
    }

    @Log(title = "库存物资", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @SaCheckPermission("inventory:material:remove")
    @DeleteMapping("/materials/{id}")
    public R<Void> removeMaterial(@PathVariable Long id) {
        inventoryService.removeMaterial(id);
        return R.ok();
    }

    @SaCheckPermission("inventory:balance:list")
    @GetMapping("/materials/{id}/ledger")
    public R<PageResult<LedgerVo>> ledger(@PathVariable Long id, LedgerQuery query, PageQuery pageQuery) {
        return R.ok(inventoryService.pageLedger(id, query, pageQuery));
    }

    @Log(title = "库存入库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:inbound:add")
    @PostMapping("/inbound-orders")
    public R<MutationVo> inbound(@Valid @RequestBody InboundSaveBo bo) {
        return R.ok(inventoryService.createInbound(bo));
    }

    @SaCheckPermission("inventory:inbound:list")
    @GetMapping("/inbound-orders")
    public R<PageResult<InboundVo>> inboundPage(OrderQuery query, PageQuery pageQuery) {
        return R.ok(inventoryService.pageInbound(query, pageQuery));
    }

    /** 查询入库单快照明细。 */
    @SaCheckPermission("inventory:inbound:list")
    @GetMapping("/inbound-orders/{id}")
    public R<InboundVo> inboundDetail(@PathVariable Long id) {
        return R.ok(correctionService.getInbound(id));
    }

    /** 查询入库单历史纠错的前后快照，不包含作废或其他库存操作日志。 */
    @SaCheckPermission("inventory:inbound:list")
    @GetMapping("/inbound-orders/{id}/correction-logs")
    public R<PageResult<InboundCorrectionLogVo>> inboundCorrectionLogs(@PathVariable Long id,
                                                                         PageQuery pageQuery) {
        return R.ok(correctionService.pageInboundCorrectionLogs(id, pageQuery));
    }

    /** 就地纠错已生效入库单和原流水。 */
    @Log(title = "库存入库纠错", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:inbound:edit")
    @PutMapping("/inbound-orders/{id}")
    public R<MutationVo> correctInbound(@PathVariable Long id,
                                        @Valid @RequestBody InboundCorrectionBo bo) {
        return R.ok(correctionService.correctInbound(id, bo));
    }

    /** 逻辑作废入库单并消除库存影响。 */
    @Log(title = "库存入库作废", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @SaCheckPermission("inventory:inbound:remove")
    @DeleteMapping("/inbound-orders/{id}")
    public R<MutationVo> deleteInbound(@PathVariable Long id, @Valid CorrectionDeleteBo bo) {
        return R.ok(correctionService.deleteInbound(id, bo));
    }

    @Log(title = "库存直接出库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:outbound:add")
    @PostMapping("/outbound-orders/direct")
    public R<MutationVo> directOutbound(@Valid @RequestBody DirectOutboundSaveBo bo) {
        return R.ok(inventoryService.createDirectOutbound(bo));
    }

    /** 查询当前租户可通过后台直接完成出库的领料单。 */
    @SaCheckPermission("inventory:outbound:add")
    @GetMapping("/outbound-orders/direct/material-receipts")
    public R<PageResult<ReceiptVo>> directOutboundReceiptPage(OrderQuery query, PageQuery pageQuery) {
        return R.ok(returnService.pageDirectOutboundReceipts(query, pageQuery));
    }

    /** 查询当前租户可通过后台直接完成出库的领料单快照明细。 */
    @SaCheckPermission("inventory:outbound:add")
    @GetMapping("/outbound-orders/direct/material-receipts/{id}")
    public R<ReceiptVo> directOutboundReceiptDetail(@PathVariable Long id) {
        return R.ok(returnService.getDirectOutboundReceipt(id));
    }

    @SaCheckPermission("inventory:outbound:list")
    @GetMapping("/outbound-orders")
    public R<PageResult<OutboundVo>> outboundPage(OrderQuery query, PageQuery pageQuery) {
        return R.ok(inventoryService.pageOutbound(query, pageQuery));
    }

    @SaCheckPermission("inventory:outbound:list")
    @GetMapping("/outbound-orders/{id}")
    public R<OutboundVo> outbound(@PathVariable Long id) {
        return R.ok(inventoryService.getOutbound(id));
    }

    /** 查询已出库单历史纠错的前后快照，不包含作废或其他库存操作日志。 */
    @SaCheckPermission("inventory:outbound:list")
    @GetMapping("/outbound-orders/{id}/correction-logs")
    public R<PageResult<OutboundCorrectionLogVo>> outboundCorrectionLogs(@PathVariable Long id,
                                                                           PageQuery pageQuery) {
        return R.ok(correctionService.pageOutboundCorrectionLogs(id, pageQuery));
    }

    /** 就地纠错已完成出库单、原流水和领料实发。 */
    @Log(title = "库存出库纠错", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:outbound:edit")
    @PutMapping("/outbound-orders/{id}")
    public R<MutationVo> correctOutbound(@PathVariable Long id,
                                         @Valid @RequestBody OutboundCorrectionBo bo) {
        return R.ok(correctionService.correctOutbound(id, bo));
    }

    /** 逻辑作废已完成出库单并恢复库存。 */
    @Log(title = "库存出库作废", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @SaCheckPermission("inventory:outbound:remove")
    @DeleteMapping("/outbound-orders/{id}")
    public R<MutationVo> deleteOutbound(@PathVariable Long id, @Valid CorrectionDeleteBo bo) {
        return R.ok(correctionService.deleteOutbound(id, bo));
    }

    @Log(title = "库存出库确认", businessType = BusinessType.UPDATE)
    @SaCheckPermission("inventory:outbound:confirm")
    @PostMapping("/outbound-orders/{id}/confirm")
    public R<MutationVo> confirmOutbound(@PathVariable Long id, @Valid @RequestBody OutboundConfirmBo bo) {
        return R.ok(inventoryService.confirmOutbound(id, bo));
    }

    @Log(title = "库存出库取消", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:outbound:confirm")
    @PostMapping("/outbound-orders/{id}/cancel")
    public R<MutationVo> cancelOutbound(@PathVariable Long id, @Valid @RequestBody CancelOutboundBo bo) {
        return R.ok(inventoryService.cancelOutbound(id, bo));
    }

    /** 后台直建关联退库时查询可选领料单。 */
    @SaCheckPermission("inventory:return:list")
    @GetMapping("/material-receipts")
    public R<PageResult<ReceiptVo>> receiptPage(OrderQuery query, PageQuery pageQuery) {
        return R.ok(returnService.pageReceipts(query, pageQuery, null, null));
    }

    /** 后台直建关联退库时读取领料单实发及剩余可退明细。 */
    @SaCheckPermission("inventory:return:list")
    @GetMapping("/material-receipts/{id}")
    public R<ReceiptVo> receiptDetail(@PathVariable Long id) {
        return R.ok(returnService.getReceipt(id, null, null));
    }

    @Log(title = "库存直接退库", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:return:add")
    @PostMapping("/return-orders/direct")
    public R<MutationVo> directReturn(@Valid @RequestBody DirectReturnSaveBo bo) {
        return R.ok(returnService.createDirectReturn(bo));
    }

    @SaCheckPermission("inventory:return:list")
    @GetMapping("/return-orders")
    public R<PageResult<ReturnVo>> returnPage(OrderQuery query, PageQuery pageQuery) {
        return R.ok(returnService.pageReturns(query, pageQuery, null, null));
    }

    @SaCheckPermission("inventory:return:list")
    @GetMapping("/return-orders/{id}")
    public R<ReturnVo> returnDetail(@PathVariable Long id) {
        return R.ok(returnService.getReturn(id, null, null));
    }

    /** 查询已退库单历史纠错的前后快照，不包含删除或其他库存操作日志。 */
    @SaCheckPermission("inventory:return:list")
    @GetMapping("/return-orders/{id}/correction-logs")
    public R<PageResult<ReturnCorrectionLogVo>> returnCorrectionLogs(@PathVariable Long id,
                                                                       PageQuery pageQuery) {
        return R.ok(correctionService.pageReturnCorrectionLogs(id, pageQuery));
    }

    /** 查询退库单创建、确认、纠错、作废等完整操作审计快照。 */
    @SaCheckPermission("inventory:return:list")
    @GetMapping("/return-orders/{id}/operation-logs")
    public R<PageResult<ReturnOperationLogVo>> returnOperationLogs(@PathVariable Long id,
                                                                     PageQuery pageQuery) {
        return R.ok(correctionService.pageReturnOperationLogs(id, pageQuery));
    }

    @Log(title = "库存退库确认", businessType = BusinessType.UPDATE)
    @SaCheckPermission("inventory:return:confirm")
    @PostMapping("/return-orders/{id}/confirm")
    public R<MutationVo> confirmReturn(@PathVariable Long id, @Valid @RequestBody ReturnConfirmBo bo) {
        return R.ok(returnService.confirmReturn(id, bo));
    }

    /** 就地纠错已退库单、原流水和累计退库数量。 */
    @Log(title = "库存退库纠错", businessType = BusinessType.UPDATE)
    @RepeatSubmit
    @SaCheckPermission("inventory:return:edit")
    @PutMapping("/return-orders/{id}")
    public R<MutationVo> correctReturn(@PathVariable Long id,
                                       @Valid @RequestBody ReturnCorrectionBo bo) {
        return R.ok(correctionService.correctReturn(id, bo));
    }

    /** 作废待退库或已退库单；已退库单会保留原流水并新增冲销流水。 */
    @Log(title = "库存退库作废", businessType = BusinessType.DELETE)
    @RepeatSubmit
    @SaCheckPermission("inventory:return:remove")
    @DeleteMapping("/return-orders/{id}")
    public R<MutationVo> deleteReturn(@PathVariable Long id, @Valid CorrectionDeleteBo bo) {
        return R.ok(correctionService.deleteReturn(id, bo));
    }

    @Log(title = "库存盘点", businessType = BusinessType.INSERT)
    @RepeatSubmit
    @SaCheckPermission("inventory:stocktake:add")
    @PostMapping("/stocktake-orders")
    public R<MutationVo> stocktake(@Valid @RequestBody StocktakeSaveBo bo) {
        return R.ok(inventoryService.createStocktake(bo));
    }

    @SaCheckPermission("inventory:stocktake:list")
    @GetMapping("/stocktake-orders")
    public R<PageResult<StocktakeVo>> stocktakePage(OrderQuery query, PageQuery pageQuery) {
        return R.ok(inventoryService.pageStocktake(query, pageQuery));
    }

    /** 查询已完成盘点的单据头和物资快照明细。 */
    @SaCheckPermission("inventory:stocktake:list")
    @GetMapping("/stocktake-orders/{id}")
    public R<StocktakeDetailVo> stocktakeDetail(@PathVariable Long id) {
        return R.ok(inventoryService.getStocktake(id));
    }
}
    /**
     * 新增当前租户的物料分类。
     */
    /**
     * 按版本更新当前租户的物料分类。
     */
    /**
     * 删除当前租户中从未被物料引用的分类。
     */
