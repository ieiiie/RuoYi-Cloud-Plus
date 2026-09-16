package com.ym.agriculture.farming.algback.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.agriculture.farming.algback.model.entity.SfAiInferenceLog;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceCountAggDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceLabelCountDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceLabelStatRowDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceScoreDistributionAggDto;
import com.ym.agriculture.farming.field.model.dto.FieldInferenceTimeseriesRowDto;
import com.ym.agriculture.farming.field.model.vo.SfFieldAiInferencePageVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * {@code sf_ai_inference_log}。
 * <p>
 * 地块图表相关查询使用 {@link Select} + {@code <script>}，不依赖 XML Mapper。
 */
public interface SfAiInferenceLogMapper extends BaseMapper<SfAiInferenceLog> {

    /**
     * 关联 UAV 任务表；两表字符串列应同为 utf8mb4_general_ci（见 docs/sql/sf_uav_ai_task_collation_utf8mb4_general_ci.sql）。
     */
    String JOIN_UAV_WHERE = """
        FROM sf_ai_inference_log L
        INNER JOIN sf_uav_ai_task T ON T.alg_task_no = L.task_no AND T.tenant_id = L.tenant_id
        WHERE T.del_flag = '0'
          AND T.field_id = #{fieldId}
          AND L.tenant_id = #{tenantId}
          AND T.tenant_id = #{tenantId}
        <if test="plantingBatchId != null">
            AND T.planting_batch_id = #{plantingBatchId}
        </if>
        """;

    String ALARM_RANGE = """
        <if test="startAlarmTime != null">
            AND L.alarm_time &gt;= #{startAlarmTime}
        </if>
        <if test="endAlarmTime != null">
            AND L.alarm_time &lt;= #{endAlarmTime}
        </if>
        """;

    /**
     * 按地块 JOIN UAV AI 任务后的推理流水总数（不受 alarm 时间过滤）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT COUNT(1)
        """ + JOIN_UAV_WHERE + """
        </script>
        """)
    Long countFieldInferenceByField(
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId);

    /**
     * 时间范围内的条数及去重 task_no / uav_job_id；无起止时间时不限制 alarm_time。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT COUNT(1) AS cnt,
               COUNT(DISTINCT L.task_no) AS distinctTaskNo,
               COUNT(DISTINCT T.uav_job_id) AS distinctUavJobId,
               SUM(CASE WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$' THEN 1 ELSE 0 END) AS scoreParsedCount,
               AVG(CASE WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                   THEN CAST(L.cls_score_value AS DECIMAL(18, 6)) ELSE NULL END) AS rangeAvgScore,
               MAX(CASE WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                   THEN CAST(L.cls_score_value AS DECIMAL(18, 6)) ELSE NULL END) AS rangeMaxScore,
               MIN(CASE WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                   THEN CAST(L.cls_score_value AS DECIMAL(18, 6)) ELSE NULL END) AS rangeMinScore
        """ + JOIN_UAV_WHERE + ALARM_RANGE + """
        </script>
        """)
    FieldInferenceCountAggDto selectFieldInferenceRangeAgg(
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId,
        @Param("startAlarmTime") Long startAlarmTime,
        @Param("endAlarmTime") Long endAlarmTime);

    /**
     * 时间范围内按分类标签聚合条数（摘要用）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT COALESCE(NULLIF(TRIM(L.cls_score_label), ''), '未解析') AS label,
               COUNT(1) AS cnt
        """ + JOIN_UAV_WHERE + ALARM_RANGE + """
        GROUP BY COALESCE(NULLIF(TRIM(L.cls_score_label), ''), '未解析')
        ORDER BY cnt DESC
        </script>
        """)
    List<FieldInferenceLabelCountDto> selectFieldInferenceLabelCountsForSummary(
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId,
        @Param("startAlarmTime") Long startAlarmTime,
        @Param("endAlarmTime") Long endAlarmTime);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT
        <choose>
            <when test="granularity == 'hour'">
                DATE_FORMAT(FROM_UNIXTIME(FLOOR(L.alarm_time / 1000)), '%Y-%m-%d %H:00:00') AS bucketStart,
            </when>
            <otherwise>
                DATE_FORMAT(FROM_UNIXTIME(FLOOR(L.alarm_time / 1000)), '%Y-%m-%d') AS bucketStart,
            </otherwise>
        </choose>
        COUNT(1) AS cnt,
        AVG(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    THEN CAST(L.cls_score_value AS DECIMAL(18, 6))
                ELSE NULL END) AS avgScore
        """ + JOIN_UAV_WHERE + ALARM_RANGE + """
        GROUP BY
        <choose>
            <when test="granularity == 'hour'">
                DATE_FORMAT(FROM_UNIXTIME(FLOOR(L.alarm_time / 1000)), '%Y-%m-%d %H:00:00')
            </when>
            <otherwise>
                DATE_FORMAT(FROM_UNIXTIME(FLOOR(L.alarm_time / 1000)), '%Y-%m-%d')
            </otherwise>
        </choose>
        ORDER BY 1 ASC
        </script>
        """)
    List<FieldInferenceTimeseriesRowDto> selectFieldInferenceTimeseries(
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId,
        @Param("startAlarmTime") Long startAlarmTime,
        @Param("endAlarmTime") Long endAlarmTime,
        @Param("granularity") String granularity);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT COALESCE(NULLIF(TRIM(L.cls_score_label), ''), '未解析') AS label,
               COUNT(1) AS cnt,
               AVG(CASE
                       WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                           THEN CAST(L.cls_score_value AS DECIMAL(18, 6))
                       ELSE NULL END) AS avgScore,
               MAX(CASE
                       WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                           THEN CAST(L.cls_score_value AS DECIMAL(18, 6))
                       ELSE NULL END) AS maxScore
        """ + JOIN_UAV_WHERE + ALARM_RANGE + """
        GROUP BY COALESCE(NULLIF(TRIM(L.cls_score_label), ''), '未解析')
        ORDER BY cnt DESC
        </script>
        """)
    List<FieldInferenceLabelStatRowDto> selectFieldInferenceLabelStats(
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId,
        @Param("startAlarmTime") Long startAlarmTime,
        @Param("endAlarmTime") Long endAlarmTime);

    /**
     * 时间范围内可解析置信度在 [0,1] 五分桶及桶外条数（单行聚合）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT
        SUM(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &gt;= 0
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &lt; 0.2
                THEN 1 ELSE 0 END) AS b00,
        SUM(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &gt;= 0.2
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &lt; 0.4
                THEN 1 ELSE 0 END) AS b02,
        SUM(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &gt;= 0.4
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &lt; 0.6
                THEN 1 ELSE 0 END) AS b04,
        SUM(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &gt;= 0.6
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &lt; 0.8
                THEN 1 ELSE 0 END) AS b06,
        SUM(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &gt;= 0.8
                    AND CAST(L.cls_score_value AS DECIMAL(18, 6)) &lt;= 1
                THEN 1 ELSE 0 END) AS b08,
        SUM(CASE
                WHEN L.cls_score_value REGEXP '^-?[0-9]+(\\.[0-9]+)?$'
                    AND (CAST(L.cls_score_value AS DECIMAL(18, 6)) &lt; 0
                    OR CAST(L.cls_score_value AS DECIMAL(18, 6)) &gt; 1)
                THEN 1 ELSE 0 END) AS bOther
        """ + JOIN_UAV_WHERE + ALARM_RANGE + """
        </script>
        """)
    FieldInferenceScoreDistributionAggDto selectFieldInferenceScoreDistribution(
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId,
        @Param("startAlarmTime") Long startAlarmTime,
        @Param("endAlarmTime") Long endAlarmTime);

    @InterceptorIgnore(tenantLine = "true")
    @Select("""
        <script>
        SELECT L.id,
               L.alarm_time AS alarmTime,
               L.create_time AS createTime,
               L.cls_score_label AS clsScoreLabel,
               L.cls_score_value AS clsScoreValue,
               L.cls_score AS clsScoreRaw,
               L.img_url AS imgUrl,
               L.uav_media_id AS uavMediaId,
               L.source_file_id AS sourceFileId,
               L.source_file_name AS sourceFileName,
               L.source_object_key AS sourceObjectKey,
               L.shoot_lat AS shootLat,
               L.shoot_lng AS shootLng,
               L.shoot_time AS shootTime,
               L.task_no AS taskNo,
               T.uav_job_id AS uavJobId,
               L.model_no AS modelNo,
               L.model_name AS modelName,
               L.task_name AS taskName,
               L.algorithm_type_value AS algorithmTypeValue
        """ + JOIN_UAV_WHERE + ALARM_RANGE + """
        <if test="taskNo != null and taskNo != ''">
            AND L.task_no LIKE CONCAT('%', #{taskNo}, '%')
        </if>
        <if test="modelNo != null and modelNo != ''">
            AND L.model_no LIKE CONCAT('%', #{modelNo}, '%')
        </if>
        <if test="labelKeyword != null and labelKeyword != ''">
            AND L.cls_score_label LIKE CONCAT('%', #{labelKeyword}, '%')
        </if>
        ORDER BY L.alarm_time DESC, L.id DESC
        </script>
        """)
    Page<SfFieldAiInferencePageVo> selectFieldInferencePage(
        Page<SfFieldAiInferencePageVo> page,
        @Param("fieldId") Long fieldId,
        @Param("tenantId") String tenantId,
        @Param("plantingBatchId") Long plantingBatchId,
        @Param("startAlarmTime") Long startAlarmTime,
        @Param("endAlarmTime") Long endAlarmTime,
        @Param("taskNo") String taskNo,
        @Param("modelNo") String modelNo,
        @Param("labelKeyword") String labelKeyword);

    /**
     * AI 任务详情页关联推理日志（有上限）。
     */
    default List<SfAiInferenceLog> selectListForAiTaskDetail(String taskNo, String tenantId, int limit) {
        LambdaQueryWrapper<SfAiInferenceLog> w = Wrappers.lambdaQuery();
        w.eq(SfAiInferenceLog::getTaskNo, taskNo.trim());
        w.eq(SfAiInferenceLog::getTenantId, tenantId);
        w.orderByDesc(SfAiInferenceLog::getAlarmTime);
        w.orderByDesc(SfAiInferenceLog::getCreateTime);
        w.last("LIMIT " + limit);
        return selectList(w);
    }
}
