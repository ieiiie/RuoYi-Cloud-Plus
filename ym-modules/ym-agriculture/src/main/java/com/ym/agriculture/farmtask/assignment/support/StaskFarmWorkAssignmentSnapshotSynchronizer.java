package com.ym.agriculture.farmtask.assignment.support;

import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farming.farmwork.support.FarmWorkAssignmentSnapshotSynchronizer;
import com.ym.agriculture.farmtask.assignment.dao.SfFarmWorkAssignmentMapper;
import com.ym.agriculture.farmtask.i18n.StaskI18nResourceRegistrar;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * stask 农事分配记录的字典快照同步实现。
 */
@Component
@RequiredArgsConstructor
public class StaskFarmWorkAssignmentSnapshotSynchronizer implements FarmWorkAssignmentSnapshotSynchronizer {

    private final SfFarmWorkAssignmentMapper assignmentMapper;
    @Autowired
    private StaskI18nResourceRegistrar i18nResourceRegistrar;

    @Override
    public void synchronize(String tenantId, SfFarmWorkDict dict, SfFarmWorkDict category) {
        if (FarmWorkNodeType.isItem(dict.getNodeType())) {
            assignmentMapper.updateWorkItemSnapshot(
                tenantId,
                dict.getDictId(),
                dict.getDictName(),
                dict.getDictCode(),
                dict.getParentId(),
                category == null ? null : category.getDictName()
            );
            registerAssignments(tenantId, assignmentMapper.selectByWorkItemId(tenantId, dict.getDictId()));
            return;
        }
        if (FarmWorkNodeType.isCategory(dict.getNodeType())) {
            assignmentMapper.updateCategorySnapshot(tenantId, dict.getDictId(), dict.getDictName());
            registerAssignments(tenantId, assignmentMapper.selectByCategoryIdSnapshot(tenantId, dict.getDictId()));
        }
    }

    private void registerAssignments(String tenantId, java.util.Collection<com.ym.agriculture.farmtask.assignment.model.entity.SfFarmWorkAssignment> rows) {
        if (i18nResourceRegistrar != null) {
            i18nResourceRegistrar.registerAssignments(tenantId, rows);
        }
    }
}
