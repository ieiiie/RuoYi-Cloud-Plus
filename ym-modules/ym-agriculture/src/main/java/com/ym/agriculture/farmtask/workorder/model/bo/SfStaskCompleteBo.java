package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskCompletion;
import io.github.linpeilie.annotations.AutoMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * stask 组长提交完工入参。
 */
@Data
@AutoMapper(target = SfStaskCompletion.class, reverseConvertGenerate = false)
public class SfStaskCompleteBo {

    /**
     * 作业照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    @NotBlank(message = "{" + StaskMessageKeys.VALIDATION_WORK_PHOTO_REQUIRED + "}")
    private String workPhotos;

    /**
     * 完工备注。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_COMPLETION_REMARK_MAX + "}")
    private String completionRemark;
}
