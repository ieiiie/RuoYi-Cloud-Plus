package com.ym.agriculture.farming.field.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;

import java.util.List;

/**
 * 地块地图/列表统一展示状态（{@code mapDisplayStatus}）
 * <p>
 * 优先级：档案停用 {@link #DISABLED}；否则若 {@code field_status=MAINTENANCE} 为 {@link #MAINTENANCE}（压过进行中批次）；
 * 否则按进行中批次优先级；再回退 {@code field_status}（{@code IDLE}/{@code IN_USE} 等）；默认 {@code IDLE}。
 *
 * @author ym-cloud
 */
public final class FieldMapDisplayStatus {

    /**
     * 档案停用时统一展示值（对应 sf_field.status = 1）。
     */
    public static final String DISABLED = "DISABLED";

    /**
     * 业务维护中（与 {@code sf_field.field_status=MAINTENANCE} 一致），优先于进行中种植批次展示。
     */
    public static final String MAINTENANCE = "MAINTENANCE";

    private FieldMapDisplayStatus() {
    }

    /**
     * 计算单块地的地图/列表展示状态。
     *
     * @param archiveStatus 档案启用：0 正常，1 停用（sf_field.status）
     * @param fieldStatus   业务占用：IDLE / IN_USE / MAINTENANCE（sf_field.field_status）
     * @param batchStatuses 该地块下未删除批次的 batch_status 列表；终态如 FINISHED、FAILED 不参与展示优先级
     * @return 统一枚举串，如 DISABLED、PLANNING、PLANTING、IDLE、IN_USE 等
     */
    public static String compute(String archiveStatus, String fieldStatus, List<String> batchStatuses) {
        if ("1".equals(archiveStatus)) {
            return DISABLED;
        }
        if (StringUtils.isNotBlank(fieldStatus) && MAINTENANCE.equals(fieldStatus)) {
            return MAINTENANCE;
        }
        String active = batchStatuses == null ? null : batchStatuses.stream()
            .filter(s -> StringUtils.isNotBlank(s) && PlantingBatchStatus.isActive(s))
            .min(PlantingBatchStatus.ACTIVE_PRIORITY)
            .orElse(null);
        if (active != null) {
            return active;
        }
        if (StringUtils.isNotBlank(fieldStatus)) {
            return fieldStatus;
        }
        return "IDLE";
    }

    /**
     * 将 {@code sf_field.field_status} 转为列表/详情中文展示（无进行中批次、或非 {@link #MAINTENANCE} 时的回退文案；
     * 维护中与 {@link #compute} 一致时在服务层优先于批次展示）。
     * <p>
     * 与实体约定一致：{@code IDLE} 空闲、{@code IN_USE} 使用中、{@code MAINTENANCE} 维护中。
     *
     * @param fieldStatus 业务占用状态，允许为空
     * @return 中文标签；空时回退为「空闲」；未约定的枚举值回显原值
     */
    public static String fieldStatusToChinese(String fieldStatus) {
        if (StringUtils.isBlank(fieldStatus)) {
            return "空闲";
        }
        return switch (fieldStatus) {
            case "IDLE" -> "空闲";
            case "IN_USE" -> "使用中";
            case MAINTENANCE -> "维护中";
            default -> fieldStatus;
        };
    }
}
