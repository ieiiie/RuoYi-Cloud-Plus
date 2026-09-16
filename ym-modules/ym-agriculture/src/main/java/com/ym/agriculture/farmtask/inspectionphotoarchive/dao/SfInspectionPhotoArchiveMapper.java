package com.ym.agriculture.farmtask.inspectionphotoarchive.dao;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farmtask.inspectionphotoarchive.model.entity.SfInspectionPhotoArchive;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 巡查照片归档日期数据访问层。
 *
 * @author ym-cloud
 */
@Mapper
public interface SfInspectionPhotoArchiveMapper extends BaseMapperPlus<SfInspectionPhotoArchive, SfInspectionPhotoArchive> {

    /** 查询当前租户全部归档日期，按日期倒序。 */
    default List<SfInspectionPhotoArchive> selectDateList(String tenantId) {
        return selectList(Wrappers.<SfInspectionPhotoArchive>lambdaQuery()
            .eq(SfInspectionPhotoArchive::getTenantId, tenantId)
            .orderByDesc(SfInspectionPhotoArchive::getArchiveDate)
            .orderByDesc(SfInspectionPhotoArchive::getArchiveId));
    }

    /** 按租户和主键查询归档日期。 */
    default SfInspectionPhotoArchive selectTenantById(String tenantId, Long archiveId) {
        return selectOne(Wrappers.<SfInspectionPhotoArchive>lambdaQuery()
            .eq(SfInspectionPhotoArchive::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchive::getArchiveId, archiveId)
            .last("LIMIT 1"));
    }

    /** 按租户和日期查询归档，用于创建前的重复校验。 */
    default SfInspectionPhotoArchive selectTenantByDate(String tenantId, LocalDate archiveDate) {
        return selectOne(Wrappers.<SfInspectionPhotoArchive>lambdaQuery()
            .eq(SfInspectionPhotoArchive::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchive::getArchiveDate, archiveDate)
            .last("LIMIT 1"));
    }

    /**
     * 锁定当前租户的归档日期行，供上传、删除和同步互斥使用。
     */
    @Select("""
        SELECT *
        FROM sf_inspection_photo_archive
        WHERE tenant_id = #{tenantId}
          AND archive_id = #{archiveId}
        FOR UPDATE
        """)
    SfInspectionPhotoArchive selectTenantByIdForUpdate(@Param("tenantId") String tenantId,
                                                        @Param("archiveId") Long archiveId);

    /** 物理删除当前租户的一条归档日期。 */
    default int deleteTenantById(String tenantId, Long archiveId) {
        return delete(Wrappers.<SfInspectionPhotoArchive>lambdaQuery()
            .eq(SfInspectionPhotoArchive::getTenantId, tenantId)
            .eq(SfInspectionPhotoArchive::getArchiveId, archiveId));
    }
}
