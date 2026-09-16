package com.ym.agriculture.farmtask.inventory.support;

import com.ym.agriculture.farmtask.inventory.model.InventoryModels.ReceiptVo;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskTaskPackage;
import org.springframework.stereotype.Component;

/** 将任务包保存的最终经手技术员快照投影到领料单响应。 */
@Component
public class MaterialReceiptTechnicianAssembler {

    /**
     * 补充领料单技术员字段；历史任务包缺失时保留空值，不使用其他人员兜底。
     *
     * @param target      领料单响应
     * @param taskPackage 关联任务包，可为空
     */
    public void apply(ReceiptVo target, SfStaskTaskPackage taskPackage) {
        if (taskPackage == null) {
            return;
        }
        target.setHandlerTechnicianEmployeeId(taskPackage.getHandlerTechnicianEmployeeId());
        target.setHandlerTechnicianEmployeeName(taskPackage.getHandlerTechnicianEmployeeNameSnapshot());
    }
}
