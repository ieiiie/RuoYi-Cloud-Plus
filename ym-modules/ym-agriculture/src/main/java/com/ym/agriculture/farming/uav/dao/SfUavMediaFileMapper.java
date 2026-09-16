package com.ym.agriculture.farming.uav.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.agriculture.farming.uav.model.entity.SfUavMediaFile;

import java.util.Collections;
import java.util.List;

/**
 * {@code sf_uav_media_file}。
 *
 * @author ym-cloud
 */
public interface SfUavMediaFileMapper extends BaseMapper<SfUavMediaFile> {

    /**
     * 按 syncJobId 查询全部未删除的媒体文件（含经纬度等拍摄信息）。
     */
    default List<SfUavMediaFile> selectBySyncJobId(String tenantId, String syncJobId) {
        if (tenantId == null || syncJobId == null) {
            return Collections.emptyList();
        }
        return selectList(Wrappers.<SfUavMediaFile>lambdaQuery()
            .eq(SfUavMediaFile::getTenantId, tenantId)
            .eq(SfUavMediaFile::getSyncJobId, syncJobId)
            .eq(SfUavMediaFile::getDelFlag, SystemConstants.NORMAL));
    }

    /**
     * 统计指定任务下已同步的图片数量（排除 {@code fileType = VIDEO} 的记录）。
     * 用于在发起图片推理前校验媒体完整性。
     */
    default long countImagesBySyncJobId(String tenantId, String syncJobId) {
        if (tenantId == null || syncJobId == null) {
            return 0;
        }
        return selectCount(Wrappers.<SfUavMediaFile>lambdaQuery()
            .eq(SfUavMediaFile::getTenantId, tenantId)
            .eq(SfUavMediaFile::getSyncJobId, syncJobId)
            .ne(SfUavMediaFile::getFileType, "VIDEO")
            .eq(SfUavMediaFile::getDelFlag, SystemConstants.NORMAL));
    }
}
