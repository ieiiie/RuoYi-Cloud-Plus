package com.ym.agriculture.farmtask.inventory.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.CorrectionDeleteBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.OutboundCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.OutboundCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnCorrectionBo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnCorrectionLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnOperationLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;

/** Web 后台已生效库存单据的就地纠错与删除服务。 */
public interface IInventoryCorrectionService {

    /** 查询入库单及其物资快照明细。 */
    InboundVo getInbound(Long id);

    /** 分页查询当前租户入库单的历史纠错审计快照。 */
    PageResult<InboundCorrectionLogVo> pageInboundCorrectionLogs(Long id, PageQuery pageQuery);

    /** 分页查询当前租户已出库单的历史纠错审计快照。 */
    PageResult<OutboundCorrectionLogVo> pageOutboundCorrectionLogs(Long id, PageQuery pageQuery);

    /** 分页查询当前租户已退库单的历史纠错审计快照。 */
    PageResult<ReturnCorrectionLogVo> pageReturnCorrectionLogs(Long id, PageQuery pageQuery);

    /** 分页查询当前租户退库单的完整操作审计快照。 */
    PageResult<ReturnOperationLogVo> pageReturnOperationLogs(Long id, PageQuery pageQuery);

    /** 就地修改已生效入库单、原流水和余额缓存。 */
    MutationVo correctInbound(Long id, InboundCorrectionBo bo);

    /** 逻辑作废入库单并取消原流水库存影响。 */
    MutationVo deleteInbound(Long id, CorrectionDeleteBo bo);

    /** 就地修改已完成出库单并同步领料实发。 */
    MutationVo correctOutbound(Long id, OutboundCorrectionBo bo);

    /** 逻辑作废已完成出库单并恢复库存。 */
    MutationVo deleteOutbound(Long id, CorrectionDeleteBo bo);

    /** 就地修改已退库单并同步累计退库数量。 */
    MutationVo correctReturn(Long id, ReturnCorrectionBo bo);

    /** 作废退库单；已退库单保留原流水并新增冲销流水。 */
    MutationVo deleteReturn(Long id, CorrectionDeleteBo bo);
}
