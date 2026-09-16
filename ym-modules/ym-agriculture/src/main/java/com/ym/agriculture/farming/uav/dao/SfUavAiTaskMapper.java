package com.ym.agriculture.farming.uav.dao;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.uav.model.bo.SfUavAiTaskQueryBo;
import com.ym.agriculture.farming.uav.model.entity.SfUavAiTask;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/**
 * sf_uav_ai_task Mapper。
 */
public interface SfUavAiTaskMapper extends BaseMapper<SfUavAiTask> {

    /**
     * 可选：手工/运维补偿扫描（默认由 Redisson RDelayedQueue 触发停止，不启用周期任务）。
     */
    @Select("""
        SELECT *
        FROM sf_uav_ai_task
        WHERE del_flag = '0'
          AND status = 'SUBMITTED'
          AND scheduled_stop_at IS NOT NULL
          AND scheduled_stop_at <= #{now}
        ORDER BY scheduled_stop_at ASC
        LIMIT #{limit}
        """)
    List<SfUavAiTask> selectDueScheduledStop(@Param("now") Date now, @Param("limit") int limit);

    /** 按当前显式租户回表。 */
    @Select("SELECT * FROM sf_uav_ai_task WHERE id = #{id} AND del_flag = '0'")
    SfUavAiTask selectByIdForScheduledJob(@Param("id") Long id);

    /**
     * 本地 AI 任务分页条件；{@code allowedFieldIds} 为 {@code null} 表示超管不限制。
     */
    default LambdaQueryWrapper<SfUavAiTask> buildLocalPageWrapper(SfUavAiTaskQueryBo bo, List<Long> allowedFieldIds) {
        LambdaQueryWrapper<SfUavAiTask> w = Wrappers.lambdaQuery();
        if (allowedFieldIds != null) {
            w.in(SfUavAiTask::getFieldId, allowedFieldIds);
        }
        if (bo == null) {
            return w;
        }
        if (ObjectUtil.isNotNull(bo.getFieldId())) {
            w.eq(SfUavAiTask::getFieldId, bo.getFieldId());
        }
        if (StringUtils.isNotBlank(bo.getFieldName())) {
            w.like(SfUavAiTask::getFieldName, bo.getFieldName().trim());
        }
        if (StringUtils.isNotBlank(bo.getSpeciesName())) {
            w.like(SfUavAiTask::getSpeciesName, bo.getSpeciesName().trim());
        }
        if (StringUtils.isNotBlank(bo.getVarietyName())) {
            w.like(SfUavAiTask::getVarietyName, bo.getVarietyName().trim());
        }
        if (StringUtils.isNotBlank(bo.getPlantingBatchName())) {
            w.like(SfUavAiTask::getPlantingBatchName, bo.getPlantingBatchName().trim());
        }
        if (StringUtils.isNotBlank(bo.getStatus())) {
            w.eq(SfUavAiTask::getStatus, bo.getStatus().trim());
        }
        if (StringUtils.isNotBlank(bo.getUavJobId())) {
            w.like(SfUavAiTask::getUavJobId, bo.getUavJobId().trim());
        }
        if (StringUtils.isNotBlank(bo.getAlgTaskNo())) {
            w.like(SfUavAiTask::getAlgTaskNo, bo.getAlgTaskNo().trim());
        }
        if (StringUtils.isNotBlank(bo.getModelNo())) {
            w.eq(SfUavAiTask::getModelNo, bo.getModelNo().trim());
        }
        if (bo.getCreateTimeBegin() != null && bo.getCreateTimeEnd() != null) {
            w.between(SfUavAiTask::getCreateTime, bo.getCreateTimeBegin(), bo.getCreateTimeEnd());
        } else if (bo.getCreateTimeBegin() != null) {
            w.ge(SfUavAiTask::getCreateTime, bo.getCreateTimeBegin());
        } else if (bo.getCreateTimeEnd() != null) {
            w.le(SfUavAiTask::getCreateTime, bo.getCreateTimeEnd());
        }
        return w;
    }

    default SfUavAiTask selectLatestOneByUavJobId(String uavJobId, List<Long> allowedFieldIds) {
        LambdaQueryWrapper<SfUavAiTask> w = Wrappers.lambdaQuery();
        w.eq(SfUavAiTask::getUavJobId, uavJobId.trim());
        if (allowedFieldIds != null) {
            w.in(SfUavAiTask::getFieldId, allowedFieldIds);
        }
        w.orderByDesc(SfUavAiTask::getCreateTime);
        w.orderByDesc(SfUavAiTask::getId);
        w.last("LIMIT 1");
        return selectOne(w);
    }
}
