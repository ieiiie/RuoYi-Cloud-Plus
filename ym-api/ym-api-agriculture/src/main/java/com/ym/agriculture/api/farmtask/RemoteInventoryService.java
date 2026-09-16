package com.ym.agriculture.api.farmtask;

import com.ym.agriculture.api.farmtask.domain.bo.RemoteInventoryCommandBo;
import com.ym.agriculture.api.farmtask.domain.bo.RemoteInventoryQueryBo;
import com.ym.agriculture.api.farmtask.domain.vo.RemoteInventoryViewVo;
import com.ym.common.core.domain.PageResult;

/**
 * 库存 V1.5 与领退料小程序契约。
 *
 * <p>所有权限、租户、人员角色与单据状态均在农业 Provider 内重新校验。</p>
 */
public interface RemoteInventoryService {

    RemoteInventoryViewVo workbench();

    PageResult<RemoteInventoryViewVo> pageMaterials(RemoteInventoryQueryBo query);

    RemoteInventoryViewVo getMaterial(Long materialId);

    PageResult<RemoteInventoryViewVo> pageLedger(Long materialId, RemoteInventoryQueryBo query);

    PageResult<RemoteInventoryViewVo> pageOutbound(RemoteInventoryQueryBo query);

    RemoteInventoryViewVo getOutbound(Long outboundId);

    RemoteInventoryViewVo savePendingOutboundLines(Long outboundId, RemoteInventoryCommandBo command);

    RemoteInventoryViewVo confirmOutbound(Long outboundId, RemoteInventoryCommandBo command);

    RemoteInventoryViewVo cancelOutbound(Long outboundId, RemoteInventoryCommandBo command);

    PageResult<RemoteInventoryViewVo> pageReceipts(RemoteInventoryQueryBo query);

    RemoteInventoryViewVo getReceipt(Long receiptId);

    RemoteInventoryViewVo applyOutbound(Long receiptId, RemoteInventoryCommandBo command);

    PageResult<RemoteInventoryViewVo> pageReturns(RemoteInventoryQueryBo query);

    RemoteInventoryViewVo getReturn(Long returnId);

    RemoteInventoryViewVo createReturn(RemoteInventoryCommandBo command);

    RemoteInventoryViewVo updateReturn(Long returnId, RemoteInventoryCommandBo command);

    void deleteReturn(Long returnId, Long version, RemoteInventoryCommandBo command);

    RemoteInventoryViewVo confirmReturn(Long returnId, RemoteInventoryCommandBo command);

    PageResult<RemoteInventoryViewVo> pageAssets(RemoteInventoryQueryBo query);

    RemoteInventoryViewVo getAsset(Long assetId);
}
