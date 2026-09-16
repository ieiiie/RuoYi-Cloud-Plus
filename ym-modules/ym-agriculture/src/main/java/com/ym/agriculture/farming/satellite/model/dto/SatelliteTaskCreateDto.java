package com.ym.agriculture.farming.satellite.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 创建遥感任务入参（平台 API，JSON 与前端一致使用 <strong>camelCase</strong>）。
 * 提交外部遥感服务时的 snake_case 由 {@link com.ym.agriculture.farming.satellite.remote.dto.SatelliteRemoteTaskRequest} 承担。
 */
@Data
public class SatelliteTaskCreateDto implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 可选：关联业务地块 */
    private Long fieldId;

    @NotNull(message = "dkGeom不能为空")
    private Object dkGeom;

    /**
     * 与 {@code sf_crop_variety.variety_code} 一致（品种编码，租户内唯一）。
     */
    @NotBlank(message = "codeCroptype不能为空")
    @Size(max = 64, message = "codeCroptype长度不能超过64")
    private String codeCroptype;

    @NotNull(message = "startDate不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date startDate;

    @NotNull(message = "endDate不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "Asia/Shanghai")
    private Date endDate;

    @NotBlank(message = "taskType不能为空")
    private String taskType;

    @Min(value = 2, message = "pixelImage必须不小于2")
    private Integer pixelImage = 10;
}
