package com.ym.agriculture.farmtask.inventory.support;

import com.ym.common.core.domain.R;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.inventory.InventoryConstants;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.InboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryCorrectionModels.ReturnOperationLogVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetDeviceVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.AssetTimelineVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.LedgerVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MiniappPageResponse;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.MutationVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.OutboundVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReturnVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeDetailVo;
import com.ym.agriculture.farmtask.inventory.model.InventoryModels.StocktakeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 将库存稳定编码投影为当前请求语言的展示标签。
 *
 * <p>本组件只修改响应 VO 的 {@code *Label} 字段，不改变任何业务编码。</p>
 */
@Component
@RequiredArgsConstructor
public class InventoryDisplayLabelResolver {

    private final StaskMessageResolver messages;

    /**
     * 递归填充统一响应、分页、集合和库存详情中的展示标签。
     *
     * @param body Controller 返回对象
     */
    public void enrich(Object body) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        enrichNode(body, visited);
    }

    private void enrichNode(Object node, Set<Object> visited) {
        if (node == null || visited.contains(node)) {
            return;
        }
        Class<?> type = node.getClass();
        if (!type.isPrimitive() && !(node instanceof String) && !(node instanceof Number)
            && !(node instanceof Boolean)) {
            visited.add(node);
        }
        if (node instanceof R<?> result) {
            enrichNode(result.getData(), visited);
            return;
        }
        if (node instanceof PageResult<?> page) {
            enrichNode(page.getRows(), visited);
            return;
        }
        if (node instanceof MiniappPageResponse<?> page) {
            enrichNode(page.getItems(), visited);
            return;
        }
        if (node instanceof Collection<?> rows) {
            rows.forEach(row -> enrichNode(row, visited));
            return;
        }
        if (node instanceof Map<?, ?> map) {
            map.values().forEach(value -> enrichNode(value, visited));
            return;
        }
        if (type.isArray()) {
            for (int index = 0; index < Array.getLength(node); index++) {
                enrichNode(Array.get(node, index), visited);
            }
            return;
        }
        if (node instanceof MutationVo vo) {
            vo.setStatusLabel(mutationStatus(vo.getStatus()));
            return;
        }
        if (node instanceof LedgerVo vo) {
            vo.setTypeLabel(ledgerType(vo.getType()));
            vo.setBusinessSubtypeLabel(businessSubtype(vo.getBusinessSubtype()));
            vo.setRelatedOrderTypeLabel(relatedOrderType(vo.getRelatedOrderType()));
            return;
        }
        if (node instanceof InboundVo vo) {
            vo.setStatusLabel(inboundStatus(vo.getStatus()));
            return;
        }
        if (node instanceof StocktakeDetailVo vo) {
            vo.setStatusLabel(stocktakeStatus(vo.getStatus()));
            return;
        }
        if (node instanceof StocktakeVo vo) {
            vo.setStatusLabel(stocktakeStatus(vo.getStatus()));
            return;
        }
        if (node instanceof OutboundVo vo) {
            vo.setSourceLabel(outboundSource(vo.getSource()));
            vo.setStatusLabel(outboundStatus(vo.getStatus()));
            return;
        }
        if (node instanceof ReceiptVo vo) {
            vo.setStatusLabel(receiptStatus(vo.getStatus()));
            return;
        }
        if (node instanceof ReturnVo vo) {
            vo.setSourceLabel(returnSource(vo.getSource()));
            vo.setStatusLabel(returnStatus(vo.getStatus()));
            return;
        }
        if (node instanceof ReturnOperationLogVo vo) {
            vo.setActionLabel(returnOperationAction(vo.getAction()));
            enrichNode(vo.getBefore(), visited);
            enrichNode(vo.getAfter(), visited);
            return;
        }
        if (node instanceof AssetDeviceVo vo) {
            vo.setStatusLabel(assetStatus(vo.getStatus()));
            enrichNode(vo.getTimeline(), visited);
            return;
        }
        if (node instanceof AssetTimelineVo vo) {
            vo.setActionLabel(assetAction(vo.getAction(), vo.getFromStatus(), vo.getToStatus()));
            vo.setFromStatusLabel(assetStatus(vo.getFromStatus()));
            vo.setToStatusLabel(assetStatus(vo.getToStatus()));
        }
    }

    String mutationStatus(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case "ENABLED" -> StaskMessageKeys.INVENTORY_LABEL_STATUS_ENABLED;
            case "DISABLED" -> StaskMessageKeys.INVENTORY_LABEL_STATUS_DISABLED;
            case "DELETED" -> StaskMessageKeys.INVENTORY_LABEL_STATUS_DELETED;
            case InventoryConstants.STATUS_PENDING -> StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING;
            case InventoryConstants.STATUS_COMPLETED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_COMPLETED;
            case InventoryConstants.STATUS_CANCELLED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_CANCELLED;
            case InventoryConstants.STATUS_VOIDED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_VOIDED;
            case InventoryConstants.STATUS_PENDING_RETURN -> StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING_RETURN;
            case InventoryConstants.STATUS_RETURNED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_RETURNED;
            case InventoryConstants.RECEIPT_PENDING_ACCEPTANCE ->
                StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING_ACCEPTANCE;
            case InventoryConstants.RECEIPT_UNCLAIMED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_UNCLAIMED;
            case InventoryConstants.RECEIPT_PENDING_OUTBOUND ->
                StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING_OUTBOUND;
            case InventoryConstants.RECEIPT_RECEIVED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_RECEIVED;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String inboundStatus(String code) {
        if (InventoryConstants.STATUS_COMPLETED.equals(code)) {
            return messages.message(StaskMessageKeys.INVENTORY_LABEL_INBOUND_COMPLETED);
        }
        if (InventoryConstants.STATUS_VOIDED.equals(code)) {
            return messages.message(StaskMessageKeys.INVENTORY_LABEL_STATUS_VOIDED);
        }
        return code;
    }

    String stocktakeStatus(String code) {
        if (InventoryConstants.STATUS_COMPLETED.equals(code)) {
            return messages.message(StaskMessageKeys.INVENTORY_LABEL_STOCKTAKE_COMPLETED);
        }
        return code;
    }

    String outboundStatus(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.STATUS_PENDING -> StaskMessageKeys.INVENTORY_LABEL_OUTBOUND_PENDING;
            case InventoryConstants.STATUS_COMPLETED -> StaskMessageKeys.INVENTORY_LABEL_OUTBOUND_COMPLETED;
            case InventoryConstants.STATUS_CANCELLED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_CANCELLED;
            case InventoryConstants.STATUS_VOIDED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_VOIDED;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String receiptStatus(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.RECEIPT_PENDING_ACCEPTANCE ->
                StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING_ACCEPTANCE;
            case InventoryConstants.RECEIPT_UNCLAIMED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_UNCLAIMED;
            case InventoryConstants.RECEIPT_PENDING_OUTBOUND ->
                StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING_OUTBOUND;
            case InventoryConstants.RECEIPT_RECEIVED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_RECEIVED;
            case InventoryConstants.RECEIPT_VOIDED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_VOIDED;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String returnStatus(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.STATUS_PENDING_RETURN -> StaskMessageKeys.INVENTORY_LABEL_STATUS_PENDING_RETURN;
            case InventoryConstants.STATUS_RETURNED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_RETURNED;
            case InventoryConstants.STATUS_VOIDED -> StaskMessageKeys.INVENTORY_LABEL_STATUS_VOIDED;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String assetStatus(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.ASSET_IDLE -> StaskMessageKeys.INVENTORY_LABEL_ASSET_IDLE;
            case InventoryConstants.ASSET_IN_USE -> StaskMessageKeys.INVENTORY_LABEL_ASSET_IN_USE;
            case InventoryConstants.ASSET_MAINTENANCE -> StaskMessageKeys.INVENTORY_LABEL_ASSET_MAINTENANCE;
            case InventoryConstants.ASSET_SCRAPPED -> StaskMessageKeys.INVENTORY_LABEL_ASSET_SCRAPPED;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String outboundSource(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.SOURCE_MATERIAL_RECEIPT ->
                StaskMessageKeys.INVENTORY_LABEL_OUTBOUND_SOURCE_MATERIAL_RECEIPT;
            case InventoryConstants.SOURCE_WEB_DIRECT ->
                StaskMessageKeys.INVENTORY_LABEL_OUTBOUND_SOURCE_WEB_DIRECT;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String returnSource(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.SOURCE_LEADER_APPLY ->
                StaskMessageKeys.INVENTORY_LABEL_RETURN_SOURCE_LEADER_APPLY;
            case InventoryConstants.SOURCE_WEB_DIRECT ->
                StaskMessageKeys.INVENTORY_LABEL_RETURN_SOURCE_WEB_DIRECT;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String ledgerType(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case InventoryConstants.LEDGER_INBOUND -> StaskMessageKeys.INVENTORY_LABEL_LEDGER_INBOUND;
            case InventoryConstants.LEDGER_OUTBOUND -> StaskMessageKeys.INVENTORY_LABEL_LEDGER_OUTBOUND;
            case InventoryConstants.LEDGER_RETURN -> StaskMessageKeys.INVENTORY_LABEL_LEDGER_RETURN;
            case InventoryConstants.LEDGER_ADJUST -> StaskMessageKeys.INVENTORY_LABEL_LEDGER_ADJUST;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String businessSubtype(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case "WEB_INBOUND" -> StaskMessageKeys.INVENTORY_LABEL_SUBTYPE_WEB_INBOUND;
            case InventoryConstants.SOURCE_WEB_DIRECT -> StaskMessageKeys.INVENTORY_LABEL_SUBTYPE_WEB_DIRECT;
            case InventoryConstants.SOURCE_MATERIAL_RECEIPT ->
                StaskMessageKeys.INVENTORY_LABEL_SUBTYPE_MATERIAL_RECEIPT;
            case InventoryConstants.SOURCE_LEADER_APPLY -> StaskMessageKeys.INVENTORY_LABEL_SUBTYPE_LEADER_APPLY;
            case "STOCKTAKE" -> StaskMessageKeys.INVENTORY_LABEL_SUBTYPE_STOCKTAKE;
            case InventoryConstants.SUBTYPE_RETURN_VOID -> StaskMessageKeys.INVENTORY_LABEL_SUBTYPE_RETURN_VOID;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String returnOperationAction(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case "CREATE" -> StaskMessageKeys.INVENTORY_LABEL_RETURN_ACTION_CREATE;
            case "UPDATE" -> StaskMessageKeys.INVENTORY_LABEL_RETURN_ACTION_UPDATE;
            case "CONFIRM" -> StaskMessageKeys.INVENTORY_LABEL_RETURN_ACTION_CONFIRM;
            case "CREATE_AND_CONFIRM" -> StaskMessageKeys.INVENTORY_LABEL_RETURN_ACTION_CREATE_AND_CONFIRM;
            case "HISTORICAL_CORRECTION" -> StaskMessageKeys.INVENTORY_LABEL_RETURN_ACTION_HISTORICAL_CORRECTION;
            case "VOID" -> StaskMessageKeys.INVENTORY_LABEL_RETURN_ACTION_VOID;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String relatedOrderType(String code) {
        if (code == null) {
            return null;
        }
        String key = switch (code) {
            case "INBOUND" -> StaskMessageKeys.INVENTORY_LABEL_ORDER_INBOUND;
            case "OUTBOUND" -> StaskMessageKeys.INVENTORY_LABEL_ORDER_OUTBOUND;
            case "RETURN" -> StaskMessageKeys.INVENTORY_LABEL_ORDER_RETURN;
            case "STOCKTAKE" -> StaskMessageKeys.INVENTORY_LABEL_ORDER_STOCKTAKE;
            default -> null;
        };
        return labelOrCode(code, key);
    }

    String assetAction(String action, String fromStatus, String toStatus) {
        if (action == null) {
            return null;
        }
        String key = switch (action) {
            case "CREATE" -> StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_CREATE;
            case "CHECK_OUT" -> StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_CHECK_OUT;
            case "RETURN" -> StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_RETURN;
            case "MAINTENANCE" -> StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_SEND_MAINTENANCE;
            case "SCRAP" -> StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_SCRAP;
            case "CHANGE_STATUS" -> assetStatusChangeAction(fromStatus, toStatus);
            default -> null;
        };
        return labelOrCode(action, key);
    }

    private String assetStatusChangeAction(String fromStatus, String toStatus) {
        if (InventoryConstants.ASSET_SCRAPPED.equals(toStatus)) {
            return StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_SCRAP;
        }
        if (InventoryConstants.ASSET_IDLE.equals(fromStatus)
            && InventoryConstants.ASSET_MAINTENANCE.equals(toStatus)) {
            return StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_SEND_MAINTENANCE;
        }
        if (InventoryConstants.ASSET_MAINTENANCE.equals(fromStatus)
            && InventoryConstants.ASSET_IDLE.equals(toStatus)) {
            return StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_COMPLETE_MAINTENANCE;
        }
        return StaskMessageKeys.INVENTORY_LABEL_ASSET_ACTION_CHANGE_STATUS;
    }

    private String labelOrCode(String code, String key) {
        return key == null ? code : messages.message(key);
    }
}
