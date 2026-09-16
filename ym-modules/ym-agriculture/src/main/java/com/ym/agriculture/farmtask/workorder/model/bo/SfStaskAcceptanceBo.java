package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskAcceptance;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * stask 验收入参。
 */
@Data
@AutoMapper(target = SfStaskAcceptance.class, reverseConvertGenerate = false)
public class SfStaskAcceptanceBo {

    /**
     * 验收结果：PASS/REJECT。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_ACCEPTANCE_RESULT_REQUIRED + "}")
    private String result;

    /**
     * 不合格原因。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_ACCEPTANCE_REJECT_REASON_MAX + "}")
    private String rejectReason;

    /**
     * 验收照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String acceptancePhotos;
}
