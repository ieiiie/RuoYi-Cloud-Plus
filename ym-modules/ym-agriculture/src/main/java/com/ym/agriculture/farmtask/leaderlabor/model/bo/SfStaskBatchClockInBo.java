package com.ym.agriculture.farmtask.leaderlabor.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 批量到岗打卡请求。 */
@Data
public class SfStaskBatchClockInBo {
    /** 任务计划日期，格式 yyyy-MM-dd。 */
    @NotNull
    private LocalDate planDate;
    /** 到岗工单ID集合。 */
    @NotEmpty
    private List<Long> orderIds;
    /** 打卡类型：GPS/PHOTO。 */
    @NotBlank
    private String clockType;
    /** GPS经度。 */
    private BigDecimal longitude;
    /** GPS纬度。 */
    private BigDecimal latitude;
    /** 照片凭证JSON数组。 */
    private String proofPhotos;
    /** 客户端幂等键。 */
    @NotBlank
    private String idempotencyKey;
}
