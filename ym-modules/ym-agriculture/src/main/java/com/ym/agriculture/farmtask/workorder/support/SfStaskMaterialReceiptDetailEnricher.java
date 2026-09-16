package com.ym.agriculture.farmtask.workorder.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farmtask.inventory.dao.InventoryMappers.MaterialReceiptMapper;
import com.ym.agriculture.farmtask.inventory.model.InventoryEntities.MaterialReceipt;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskPackageItemVo;
import com.ym.agriculture.farmtask.workorder.model.vo.SfStaskWorkOrderDetailVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 按领料单唯一业务键回填任务详情的关联单据，不以名称等展示字段推断关联关系。
 */
@Component
@RequiredArgsConstructor
public class SfStaskMaterialReceiptDetailEnricher {

    private final MaterialReceiptMapper receiptMapper;

    /**
     * 回填单个拆分工单的唯一关联领料单。
     *
     * @param order 拆分工单
     * @param detail 已装配的任务详情
     */
    public void enrichOrder(SfStaskWorkOrder order, SfStaskWorkOrderDetailVo detail) {
        detail.setMaterialReceiptId(null);
        detail.setMaterialReceiptNo(null);
        if (order.getPackageId() == null || order.getWorkItemId() == null || order.getLeaderId() == null) {
            return;
        }
        MaterialReceipt receipt = receiptMapper.selectOne(Wrappers.<MaterialReceipt>lambdaQuery()
            .eq(MaterialReceipt::getTenantId, order.getTenantId())
            .eq(MaterialReceipt::getTaskPackageId, order.getPackageId())
            .eq(MaterialReceipt::getFarmItemId, order.getWorkItemId())
            .eq(MaterialReceipt::getLeaderEmployeeId, order.getLeaderId()));
        apply(detail, receipt);
    }

    /**
     * 批量回填任务包农事项的关联领料单；任务包头不选择多张单据中的任意一张。
     *
     * @param taskPackage 任务包
     * @param detail 已装配的任务详情
     */
    public void enrichPackage(SfStaskTaskPackage taskPackage, SfStaskWorkOrderDetailVo detail) {
        detail.setMaterialReceiptId(null);
        detail.setMaterialReceiptNo(null);
        List<SfStaskPackageItemVo> items = detail.getPackageItems();
        if (items == null || items.isEmpty()) {
            return;
        }
        Map<ReceiptKey, MaterialReceipt> receiptIndex = receiptMapper.selectList(
                Wrappers.<MaterialReceipt>lambdaQuery()
                    .eq(MaterialReceipt::getTenantId, taskPackage.getTenantId())
                    .eq(MaterialReceipt::getTaskPackageId, taskPackage.getPackageId()))
            .stream().collect(Collectors.toMap(
                receipt -> new ReceiptKey(receipt.getFarmItemId(), receipt.getLeaderEmployeeId()),
                Function.identity(), (first, ignored) -> first));
        items.forEach(item -> {
            item.setMaterialReceiptId(null);
            item.setMaterialReceiptNo(null);
            if (item.getWorkItemId() == null || item.getLeaderId() == null) {
                return;
            }
            MaterialReceipt receipt = receiptIndex.get(new ReceiptKey(item.getWorkItemId(), item.getLeaderId()));
            if (receipt != null) {
                item.setMaterialReceiptId(receipt.getMaterialReceiptId());
                item.setMaterialReceiptNo(receipt.getReceiptNo());
            }
        });
    }

    private static void apply(SfStaskWorkOrderDetailVo detail, MaterialReceipt receipt) {
        if (receipt == null) {
            return;
        }
        detail.setMaterialReceiptId(receipt.getMaterialReceiptId());
        detail.setMaterialReceiptNo(receipt.getReceiptNo());
    }

    private record ReceiptKey(Long farmItemId, Long leaderEmployeeId) {
    }
}
