package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * stask 任务包创建入参。
 */
@Data
public class SfStaskWorkOrderCreateBo {

    /**
     * 计划作业日期。
     */
    @NotNull(message = "{" + StaskMessageKeys.VALIDATION_PLAN_DATE_REQUIRED + "}")
    private Date planDate;

    /**
     * 是否暂存草稿。
     */
    private Boolean draft;

    /**
     * 整体技术说明，最多500字。
     */
    @Size(max = 500, message = "{" + StaskMessageKeys.VALIDATION_OVERALL_TECH_NOTE_MAX + "}")
    private String overallTechNote;

    /**
     * 农事项清单。
     */
    @Valid
    @NotEmpty(message = "{" + StaskMessageKeys.VALIDATION_WORK_ITEMS_REQUIRED + "}")
    private List<SfStaskWorkItemCreateBo> workItems;
}
