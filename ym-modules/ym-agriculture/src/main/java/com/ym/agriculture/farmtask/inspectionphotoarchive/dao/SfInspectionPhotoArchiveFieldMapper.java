package com.ym.agriculture.farmtask.inspectionphotoarchive.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchiveField;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo.SfInspectionPhotoArchiveFieldPhotoCountVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 巡查归档大棚快照数据访问层。
 *
 * @author ym-cloud
 */
@Mapper
public interface SfInspectionPhotoArchiveFieldMapper extends BaseMapperPlus<SfInspectionPhotoArchiveField, SfInspectionPhotoArchiveField> {

    /** 按归档大棚快照统计成功关联照片数。 */
    @Select("""
        <script>
        SELECT archive_field_id AS archiveFieldId, COUNT(photo_id) AS photoCount
        FROM sf_inspection_photo_archive_photo
        WHERE tenant_id = #{tenantId}
          AND archive_field_id IN
          <foreach collection="archiveFieldIds" item="archiveFieldId" open="(" separator="," close=")">
            #{archiveFieldId}
          </foreach>
        GROUP BY archive_field_id
        </script>
        """)
    List<SfInspectionPhotoArchiveFieldPhotoCountVo> countPhotosByArchiveFieldIds(
        @Param("tenantId") String tenantId, @Param("archiveFieldIds") Collection<Long> archiveFieldIds);

    /**
     * 批量统计大棚在全部巡查归档中的照片数，供大屏列表避免逐棚查询。
     */
    @Select("""
        <script>
        SELECT archive_field.field_id AS fieldId, COUNT(photo.photo_id) AS photoCount
        FROM sf_inspection_photo_archive_field archive_field
        INNER JOIN sf_inspection_photo_archive_photo photo
            ON photo.tenant_id = archive_field.tenant_id
           AND photo.archive_field_id = archive_field.archive_field_id
        WHERE archive_field.tenant_id = #{tenantId}
          AND archive_field.field_id IN
          <foreach collection="fieldIds" item="fieldId" open="(" separator="," close=")">
            #{fieldId}
          </foreach>
        GROUP BY archive_field.field_id
        </script>
        """)
    List<SfInspectionPhotoArchiveFieldPhotoCountVo> countPhotosByFieldIds(
        @Param("tenantId") String tenantId, @Param("fieldIds") Collection<Long> fieldIds);

    /** 查询归档日期下的大棚快照，按归档时排序返回。 */
    default List<SfInspectionPhotoArchiveField> selectByArchiveId(String tenantId, Long archiveId) {
        return selectList(Wrappers.<SfInspectionPhotoArchiveField>lambdaQuery()
            .eq(SfInspectionPhotoArchiveField::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchiveField::getArchiveId, archiveId)
            .orderByAsc(SfInspectionPhotoArchiveField::getSortOrder)
            .orderByAsc(SfInspectionPhotoArchiveField::getFieldId));
    }

    /** 查询归档日期中指定大棚的快照。 */
    default SfInspectionPhotoArchiveField selectTenantArchiveField(String tenantId, Long archiveId, Long archiveFieldId) {
        return selectOne(Wrappers.<SfInspectionPhotoArchiveField>lambdaQuery()
            .eq(SfInspectionPhotoArchiveField::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchiveField::getArchiveId, archiveId)
            .eq(SfInspectionPhotoArchiveField::getArchiveFieldId, archiveFieldId)
            .last("LIMIT 1"));
    }

    /** 批量删除归档日期下的全部大棚快照。 */
    default int deleteByArchiveId(String tenantId, Long archiveId) {
        return delete(Wrappers.<SfInspectionPhotoArchiveField>lambdaQuery()
            .eq(SfInspectionPhotoArchiveField::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchiveField::getArchiveId, archiveId));
    }

    /** 删除归档日期下指定的大棚快照。 */
    default int deleteByArchiveFieldIds(String tenantId, Long archiveId, Collection<Long> archiveFieldIds) {
        if (archiveFieldIds == null || archiveFieldIds.isEmpty()) {
            return 0;
        }
        return delete(Wrappers.<SfInspectionPhotoArchiveField>lambdaQuery()
            .eq(SfInspectionPhotoArchiveField::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchiveField::getArchiveId, archiveId)
            .in(SfInspectionPhotoArchiveField::getArchiveFieldId, archiveFieldIds));
    }
}
