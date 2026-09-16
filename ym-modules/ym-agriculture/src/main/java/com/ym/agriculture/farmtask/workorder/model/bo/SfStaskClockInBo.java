package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskClockRecord;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * stask 组长到岗打卡入参。
 */
@Data
@AutoMapper(target = SfStaskClockRecord.class, reverseConvertGenerate = false)
public class SfStaskClockInBo {

    /**
     * 打卡类型：GPS/PHOTO。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_CLOCK_TYPE_REQUIRED + "}")
    private String clockType;

    /**
     * 经度。
     */
    private BigDecimal longitude;

    /**
     * 纬度。
     */
    private BigDecimal latitude;

    /**
     * 证明照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String proofPhotos;
}
