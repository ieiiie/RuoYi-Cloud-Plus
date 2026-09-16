package com.ym.agriculture.farmtask.inspectionphotoarchive.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchivePhoto;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveMiniappDateVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 巡查归档照片关联数据访问层。
 *
 * @author ym-cloud
 */
@Mapper
public interface SfInspectionPhotoArchivePhotoMapper extends BaseMapperPlus<SfInspectionPhotoArchivePhoto, SfInspectionPhotoArchivePhoto> {

    /** 查询来源大棚已有成功照片的归档日期，按日期倒序。 */
    @Select("""
        SELECT DISTINCT archive.archive_id AS archiveId, archive.archive_date AS archiveDate
        FROM sf_inspection_photo_archive archive
        INNER JOIN sf_inspection_photo_archive_field archive_field
            ON archive_field.tenant_id = archive.tenant_id
           AND archive_field.archive_id = archive.archive_id
        INNER JOIN sf_inspection_photo_archive_photo photo
            ON photo.tenant_id = archive_field.tenant_id
           AND photo.archive_field_id = archive_field.archive_field_id
        WHERE archive.tenant_id = #{tenantId}
          AND archive_field.field_id = #{fieldId}
        ORDER BY archive.archive_date DESC, archive.archive_id DESC
        """)
    List<SfInspectionPhotoArchiveMiniappDateVo> selectHistoryDatesByFieldId(@Param("tenantId") String tenantId,
                                                                              @Param("fieldId") Long fieldId);

    /** 按归档大棚快照集合查询全部照片，按快照与上传顺序排序。 */
    default List<SfInspectionPhotoArchivePhoto> selectByArchiveFieldIds(String tenantId, Collection<Long> archiveFieldIds) {
        if (archiveFieldIds == null || archiveFieldIds.isEmpty()) {
            return List.of();
        }
        return selectList(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery()
            .eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId)
            .in(SfInspectionPhotoArchivePhoto::getArchiveFieldId, archiveFieldIds)
            .orderByAsc(SfInspectionPhotoArchivePhoto::getArchiveFieldId)
            .orderByAsc(SfInspectionPhotoArchivePhoto::getSeq)
            .orderByAsc(SfInspectionPhotoArchivePhoto::getPhotoId));
    }

    /** 查询指定归档大棚下的一张照片关联。 */
    default SfInspectionPhotoArchivePhoto selectTenantArchivePhoto(String tenantId, Long archiveFieldId, Long photoId) {
        return selectOne(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery()
            .eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchivePhoto::getArchiveFieldId, archiveFieldId)
            .eq(SfInspectionPhotoArchivePhoto::getPhotoId, photoId)
            .last("LIMIT 1"));
    }

    /** 按小程序客户端幂等标识查询已关联照片。 */
    default SfInspectionPhotoArchivePhoto selectByClientUploadId(String tenantId, Long archiveFieldId,
                                                                  String clientUploadId) {
        return selectOne(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery()
            .eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchivePhoto::getArchiveFieldId, archiveFieldId)
            .eq(SfInspectionPhotoArchivePhoto::getClientUploadId, clientUploadId)
            .last("LIMIT 1"));
    }

    /** 删除指定归档大棚集合下的全部照片关联，不删除 OSS 原文件。 */
    default int deleteByArchiveFieldIds(String tenantId, Collection<Long> archiveFieldIds) {
        if (archiveFieldIds == null || archiveFieldIds.isEmpty()) {
            return 0;
        }
        return delete(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery()
            .eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId)
            .in(SfInspectionPhotoArchivePhoto::getArchiveFieldId, archiveFieldIds));
    }

    /** 解除一张照片与归档大棚的关联，不删除 OSS 原文件。 */
    default int deleteTenantArchivePhoto(String tenantId, Long archiveFieldId, Long photoId) {
        return delete(Wrappers.<SfInspectionPhotoArchivePhoto>lambdaQuery()
            .eq(SfInspectionPhotoArchivePhoto::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchivePhoto::getArchiveFieldId, archiveFieldId)
            .eq(SfInspectionPhotoArchivePhoto::getPhotoId, photoId));
    }
}
