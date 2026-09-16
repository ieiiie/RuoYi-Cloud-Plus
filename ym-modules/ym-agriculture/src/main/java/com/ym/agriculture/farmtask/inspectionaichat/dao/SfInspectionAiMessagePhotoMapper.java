package com.ym.agriculture.farmtask.inspectionaichat.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionaichat.model.entity.SfInspectionAiMessagePhoto;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/** 巡查照片 AI 消息附件数据访问。 */
@Mapper
public interface SfInspectionAiMessagePhotoMapper extends BaseMapperPlus<SfInspectionAiMessagePhoto, SfInspectionAiMessagePhoto> {
    /** 批量读取消息附件。 */
    default List<SfInspectionAiMessagePhoto> selectByMessageIds(String tenantId, Collection<Long> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) return List.of();
        return selectList(Wrappers.<SfInspectionAiMessagePhoto>lambdaQuery()
            .eq(SfInspectionAiMessagePhoto::getTenantId, tenantId)
            .in(SfInspectionAiMessagePhoto::getMessageId, messageIds)
            .orderByAsc(SfInspectionAiMessagePhoto::getSortOrder));
    }
}
