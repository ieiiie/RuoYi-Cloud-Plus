package com.ym.agriculture.farmtask.workorder.model.vo;

import com.ym.agriculture.shared.i18n.annotation.StaskI18nField;
import com.ym.agriculture.shared.i18n.model.constants.I18nResourceType;
import lombok.Data;

import java.util.Date;

/**
 * stask 工单完工资料历史版本。
 */
@Data
public class SfStaskCompletionHistoryVo {

    /**
     * 完工记录主键。
     */
    private Long completionId;

    /**
     * 作业照片，JSON数组，元素为OSS文件ID或文件对象。
     */
    private String workPhotos;

    /**
     * 完工备注。
     */
    @StaskI18nField(resourceType = I18nResourceType.STASK_COMPLETION,
        idProperty = "completionId", fieldKey = "completionRemark")
    private String completionRemark;

    /**
     * 本版本提交时间。
     */
    private Date completedAt;
}
