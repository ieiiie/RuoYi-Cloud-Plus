package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * stask 技术员农事项说明入参。
 */
@Data
public class SfStaskTechInstructionBo {

    /**
     * 任务包农事项明细ID。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_WORK_ITEM_DETAIL_ID_REQUIRED + "}")
    private Long itemId;

    /**
     * 技术员针对农事项说明，选填。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_TECH_INSTRUCTION_MAX + "}")
    private String techInstruction;

    /**
     * 技术员参考照片，JSON数组，元素为OSS文件ID或文件对象，选填。
     */
    private String techPhotos;
}
