package com.ym.agriculture.farming.farmwork.support;

import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;

/**
 * 农事字典变更后的外部快照同步扩展点。
 */
public interface FarmWorkAssignmentSnapshotSynchronizer {

    /**
     * 在农事字典更新所在事务内同步依赖该字典的快照。
     *
     * @param tenantId 租户编号
     * @param dict 更新后的字典节点
     * @param category 项目所属分类；更新分类时为 {@code null}
     */
    void synchronize(String tenantId, SfFarmWorkDict dict, SfFarmWorkDict category);
}
