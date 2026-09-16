package com.ym.agriculture.farmtask.inventory.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.DirectReturnSaveBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OrderQuery;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptApplyBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnConfirmBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnCreateBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnUpdateBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnVo;

public interface IMaterialReceiptReturnService {

    /** 分页查询当前租户可由后台直接完成出库的领料单。 */
    PageResult<ReceiptVo> pageDirectOutboundReceipts(OrderQuery query, PageQuery pageQuery);

    /** 读取当前租户可由后台直接完成出库的领料单明细。 */
    ReceiptVo getDirectOutboundReceipt(Long id);

    PageResult<ReceiptVo> pageReceipts(OrderQuery query, PageQuery pageQuery, String roleCode, Long employeeId);

    ReceiptVo getReceipt(Long id, String roleCode, Long employeeId);

    MutationVo applyOutbound(Long id, ReceiptApplyBo bo, Long employeeId);

    PageResult<ReturnVo> pageReturns(OrderQuery query, PageQuery pageQuery, String roleCode, Long employeeId);

    ReturnVo getReturn(Long id, String roleCode, Long employeeId);

    MutationVo createReturn(ReturnCreateBo bo, Long leaderEmployeeId);

    MutationVo updateReturn(Long id, ReturnUpdateBo bo, Long leaderEmployeeId);

    void deleteReturn(Long id, Long version, Long employeeId, boolean keeper);

    MutationVo confirmReturn(Long id, ReturnConfirmBo bo);

    MutationVo createDirectReturn(DirectReturnSaveBo bo);

    long pendingReturnCount();
}
