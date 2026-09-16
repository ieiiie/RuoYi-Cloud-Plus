package com.ym.agriculture.farming.farmrecord.dao;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.farmrecord.model.constant.FarmingRecordStatus;
import com.ym.agriculture.farming.farmrecord.model.entity.SfFarmingRecord;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 农事记录主表 Mapper，数据源 smart-farming。
 *
 * @author ym-cloud
 */
public interface SfFarmingRecordMapper extends BaseMapperPlus<SfFarmingRecord, SfFarmingRecord> {

    /**
     * 移动端历史时间线：已提交记录，按 submit_time/record_id 键集分页。
     *
     * @param tenantId         租户编号
     * @param recordIds        记录 ID 范围，为空时返回空列表
     * @param cursorSubmitTime 游标提交时间
     * @param cursorRecordId   游标记录 ID
     * @param limit            最大返回条数
     * @return 已提交农事记录列表
     */
    default List<SfFarmingRecord> selectSubmittedTimelineKeyset(String tenantId,
                                                                  Collection<Long> recordIds,
                                                                  Date cursorSubmitTime,
                                                                  Long cursorRecordId,
                                                                  int limit) {
        if (recordIds == null || recordIds.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<SfFarmingRecord> w = Wrappers.<SfFarmingRecord>lambdaQuery()
            .eq(SfFarmingRecord::getTenantId, tenantId)
            .in(SfFarmingRecord::getRecordId, recordIds)
            .eq(SfFarmingRecord::getStatus, FarmingRecordStatus.SUBMITTED)
            .eq(SfFarmingRecord::getDelFlag, SystemConstants.NORMAL);
        if (cursorSubmitTime != null && cursorRecordId != null) {
            w.and(q -> q.lt(SfFarmingRecord::getSubmitTime, cursorSubmitTime)
                .or(n -> n.eq(SfFarmingRecord::getSubmitTime, cursorSubmitTime)
                    .lt(SfFarmingRecord::getRecordId, cursorRecordId)));
        }
        w.orderByDesc(SfFarmingRecord::getSubmitTime).orderByDesc(SfFarmingRecord::getRecordId);
        w.last("LIMIT " + Math.max(1, Math.min(limit, 300)));
        return selectList(w);
    }

    /**
     * 查询当前用户拥有的草稿。
     */
    default SfFarmingRecord selectDraftOwned(Long recordId, String tenantId, Long userId) {
        return selectOne(Wrappers.<SfFarmingRecord>lambdaQuery()
            .eq(SfFarmingRecord::getRecordId, recordId)
            .eq(SfFarmingRecord::getTenantId, tenantId)
            .eq(SfFarmingRecord::getCreateBy, userId)
            .eq(SfFarmingRecord::getStatus, FarmingRecordStatus.DRAFT)
            .eq(SfFarmingRecord::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 查询当前用户创建的已提交记录，用于纠错和删除。
     */
    default SfFarmingRecord selectSubmittedOwned(Long recordId, String tenantId, Long userId) {
        return selectOne(Wrappers.<SfFarmingRecord>lambdaQuery()
            .eq(SfFarmingRecord::getRecordId, recordId)
            .eq(SfFarmingRecord::getTenantId, tenantId)
            .eq(SfFarmingRecord::getCreateBy, userId)
            .eq(SfFarmingRecord::getStatus, FarmingRecordStatus.SUBMITTED)
            .eq(SfFarmingRecord::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 查询当前租户的已提交记录，不限制创建人；仅供超级管理员的管理后台编辑授权使用。
     */
    default SfFarmingRecord selectSubmittedInTenant(Long recordId, String tenantId) {
        return selectOne(Wrappers.<SfFarmingRecord>lambdaQuery()
            .eq(SfFarmingRecord::getRecordId, recordId)
            .eq(SfFarmingRecord::getTenantId, tenantId)
            .eq(SfFarmingRecord::getStatus, FarmingRecordStatus.SUBMITTED)
            .eq(SfFarmingRecord::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 移动端农事记录分页基础范围：已提交全租户可见，草稿仅创建者可见。
     */
    default LambdaQueryWrapper<SfFarmingRecord> wrapMobileScope(String tenantId,
                                                                 Long currentUserId,
                                                                 Collection<Long> recordIds,
                                                                 String status,
                                                                 Date fromHappenedAt,
                                                                 Date toHappenedAt) {
        LambdaQueryWrapper<SfFarmingRecord> w = Wrappers.<SfFarmingRecord>lambdaQuery()
            .eq(SfFarmingRecord::getTenantId, tenantId)
            .eq(SfFarmingRecord::getDelFlag, SystemConstants.NORMAL);
        if (recordIds != null) {
            if (recordIds.isEmpty()) {
                w.apply("1=0");
            } else {
                w.in(SfFarmingRecord::getRecordId, recordIds);
            }
        }
        if (fromHappenedAt != null) {
            w.ge(SfFarmingRecord::getHappenedAt, fromHappenedAt);
        }
        if (toHappenedAt != null) {
            w.le(SfFarmingRecord::getHappenedAt, toHappenedAt);
        }
        if (StringUtils.isNotBlank(status)) {
            String st = status.trim();
            w.eq(SfFarmingRecord::getStatus, st);
            if (FarmingRecordStatus.DRAFT.equalsIgnoreCase(st)) {
                w.eq(SfFarmingRecord::getCreateBy, currentUserId);
            }
            return w;
        }
        w.and(q -> q.eq(SfFarmingRecord::getStatus, FarmingRecordStatus.SUBMITTED)
            .or(sub -> sub.eq(SfFarmingRecord::getStatus, FarmingRecordStatus.DRAFT)
                .eq(SfFarmingRecord::getCreateBy, currentUserId)));
        return w;
    }

    /**
     * 管理后台已提交农事记录分页基础范围。
     */
    default LambdaQueryWrapper<SfFarmingRecord> wrapAdminSubmittedScope(String tenantId,
                                                                        Collection<Long> recordIds,
                                                                        Date fromHappenedAt,
                                                                        Date toHappenedAt) {
        LambdaQueryWrapper<SfFarmingRecord> w = Wrappers.<SfFarmingRecord>lambdaQuery()
            .eq(SfFarmingRecord::getTenantId, tenantId)
            .eq(SfFarmingRecord::getStatus, FarmingRecordStatus.SUBMITTED)
            .eq(SfFarmingRecord::getDelFlag, SystemConstants.NORMAL);
        if (recordIds != null) {
            if (recordIds.isEmpty()) {
                w.apply("1=0");
            } else {
                w.in(SfFarmingRecord::getRecordId, recordIds);
            }
        }
        if (fromHappenedAt != null) {
            w.ge(SfFarmingRecord::getHappenedAt, fromHappenedAt);
        }
        if (toHappenedAt != null) {
            w.le(SfFarmingRecord::getHappenedAt, toHappenedAt);
        }
        return w;
    }
}
