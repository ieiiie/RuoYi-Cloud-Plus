package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * stask 技术员确认任务包入参。
 */
@Data
public class SfStaskTechConfirmBo {

    /**
     * 农事项技术说明列表，选填；不传或传空列表均可。
     */
    @Valid
    private List<SfStaskTechInstructionBo> instructions;

    /**
     * 整体技术说明，选填。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_OVERALL_TECH_NOTE_MAX + "}")
    private String overallTechNote;
}
