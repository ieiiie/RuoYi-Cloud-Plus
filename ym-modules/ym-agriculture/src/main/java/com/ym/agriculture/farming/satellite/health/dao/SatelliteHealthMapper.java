package com.ym.agriculture.farming.satellite.health.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.satellite.health.model.SatelliteHealthResultRow;
import com.ym.agriculture.farming.satellite.health.model.vo.SatelliteHealthVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/** 卫星综合健康评分专用只读 Mapper。 */
@Mapper
public interface SatelliteHealthMapper {

    /** 按统一稳定排序分页查询启用地块。 */
    @Select("""
        <script>
        SELECT field_id, field_code, field_name, field_type, area_mu
        FROM sf_field
        WHERE tenant_id = #{tenantId} AND del_flag = '0' AND status = '0' AND field_type IN ('FIELD', 'GREENHOUSE')
        <if test="keyword != null and keyword != ''">
          AND (field_name LIKE CONCAT('%', #{keyword}, '%') OR field_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY sort_order IS NULL, sort_order ASC, field_code IS NULL, field_code ASC, field_id ASC
        </script>
        """)
    Page<SatelliteHealthVo.Field> selectFieldPage(Page<SatelliteHealthVo.Field> page,
                                                   @Param("tenantId") String tenantId,
                                                   @Param("keyword") String keyword);

    /** 查询地块是否在本租户且为启用状态。 */
    @Select("""
        SELECT field_id, field_code, field_name, field_type, area_mu
        FROM sf_field WHERE tenant_id = #{tenantId} AND field_id = #{fieldId}
          AND del_flag = '0' AND status = '0' AND field_type IN ('FIELD', 'GREENHOUSE')
        """)
    SatelliteHealthVo.Field selectEnabledField(@Param("tenantId") String tenantId, @Param("fieldId") Long fieldId);

    /** 返回目标地块的统一排序序号（从 1 开始）。 */
    @Select("""
        SELECT position_no FROM (
          SELECT field_id, ROW_NUMBER() OVER (
            ORDER BY sort_order IS NULL, sort_order ASC, field_code IS NULL, field_code ASC, field_id ASC
          ) position_no
          FROM sf_field WHERE tenant_id = #{tenantId} AND del_flag = '0' AND status = '0' AND field_type IN ('FIELD', 'GREENHOUSE')
        ) ranked WHERE field_id = #{fieldId}
        """)
    Long selectFieldPosition(@Param("tenantId") String tenantId, @Param("fieldId") Long fieldId);

    /** 批量拉取候选影像日期（原值，调用方负责日期格式校验）。 */
    @Select("""
        <script>
        SELECT DISTINCT r.image_date
        FROM sf_satellite_task_result r
        INNER JOIN sf_satellite_task t ON t.dk_id = r.dk_id AND t.tenant_id = r.tenant_id AND t.del_flag = '0'
        WHERE r.tenant_id = #{tenantId} AND r.planting_batch_id = #{batchId}
          AND t.field_id = #{fieldId} AND t.planting_batch_id = #{batchId}
          AND r.del_flag = '0' AND r.success = 1
          AND r.task_type IN ('growth','chlorophyll','nitrogen','droughtlevel','soilmoisture','soilmoisturel','health','seedlinggrowth')
        <if test="beforeDate != null and beforeDate != ''">AND r.image_date &lt; #{beforeDate}</if>
        ORDER BY r.image_date DESC LIMIT #{limit}
        </script>
        """)
    List<String> selectCandidateDates(@Param("tenantId") String tenantId, @Param("fieldId") Long fieldId,
                                      @Param("batchId") Long batchId, @Param("beforeDate") String beforeDate,
                                      @Param("limit") int limit);

    /** 一次查询候选日期的全部成功结果，按最新结果优先。 */
    @Select("""
        <script>
        SELECT r.result_id,
               CASE WHEN r.task_type = 'soilmoisturel' THEN 'soilmoisture' ELSE r.task_type END AS task_type,
               r.image_date, r.area, r.oss_url, r.create_time
        FROM sf_satellite_task_result r
        INNER JOIN sf_satellite_task t ON t.dk_id = r.dk_id AND t.tenant_id = r.tenant_id AND t.del_flag = '0'
        WHERE r.tenant_id = #{tenantId} AND r.planting_batch_id = #{batchId}
          AND t.field_id = #{fieldId} AND t.planting_batch_id = #{batchId}
          AND r.del_flag = '0' AND r.success = 1
          AND r.task_type IN
          <foreach collection="types" item="type" open="(" separator="," close=")">#{type}</foreach>
          AND r.image_date IN
          <foreach collection="dates" item="date" open="(" separator="," close=")">#{date}</foreach>
        ORDER BY r.image_date DESC,
                 CASE WHEN r.task_type = 'soilmoisturel' THEN 'soilmoisture' ELSE r.task_type END ASC,
                 r.create_time DESC, r.result_id DESC
        </script>
        """)
    List<SatelliteHealthResultRow> selectResultsByDates(@Param("tenantId") String tenantId,
                                                         @Param("fieldId") Long fieldId,
                                                         @Param("batchId") Long batchId,
                                                         @Param("dates") Collection<String> dates,
                                                         @Param("types") Collection<String> types);
}
