package com.ym.agriculture.farmtask.inventory.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DirectOutboundSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.InboundSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.CancelOutboundBo;
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
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeDetailVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.WorkbenchVo;

import java.util.List;

public interface IInventoryService {

    /** 查询当前租户的全部物料分类。 */
    List<MaterialCategoryVo> listMaterialCategories();

    /** 新增物料分类。 */
    MutationVo addMaterialCategory(MaterialCategorySaveBo bo);

    /** 按版本更新物料分类。 */
    MutationVo updateMaterialCategory(Long categoryId, MaterialCategorySaveBo bo);

    /** 按版本删除从未被物料引用的分类。 */
    void removeMaterialCategory(Long categoryId, Long version);

    PageResult<MaterialVo> pageMaterials(MaterialQuery query, PageQuery pageQuery);

    MaterialVo getMaterial(Long materialId);

    MutationVo addMaterial(MaterialSaveBo bo);

    MutationVo updateMaterial(Long materialId, MaterialSaveBo bo);

    void removeMaterial(Long materialId);

    PageResult<LedgerVo> pageLedger(Long materialId, LedgerQuery query, PageQuery pageQuery);

    WorkbenchVo workbench();

    MutationVo createInbound(InboundSaveBo bo);

    PageResult<InboundVo> pageInbound(OrderQuery query, PageQuery pageQuery);

    MutationVo createDirectOutbound(DirectOutboundSaveBo bo);

    MutationVo createPendingOutboundFromReceipt(Long receiptId);

    PageResult<OutboundVo> pageOutbound(OrderQuery query, PageQuery pageQuery);

    OutboundVo getOutbound(Long id);

    MutationVo savePendingOutboundLines(Long id, PendingLinesSaveBo bo);

    MutationVo confirmOutbound(Long id, OutboundConfirmBo bo);

    MutationVo cancelOutbound(Long id, CancelOutboundBo bo);

    MutationVo createStocktake(StocktakeSaveBo bo);

    PageResult<StocktakeVo> pageStocktake(OrderQuery query, PageQuery pageQuery);

    /** 按当前登录租户读取已完成盘点的只读快照详情。 */
    StocktakeDetailVo getStocktake(Long id);
}
