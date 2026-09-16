package com.ym.agriculture.farmtask.inspectionphotoarchive.model.bo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 巡查归档大棚同步入参。
 *
 * @author ym-cloud
 */
@Data
public class SfInspectionPhotoArchiveSyncBo {

    /** 同步动作：ADD-仅补充新增大棚，REMOVE-仅删除减少且无照片的大棚，ALL-同时执行两种操作。 */
    @NotBlank(message = "同步动作不能为空")
    @Pattern(regexp = "^(ADD|REMOVE|ALL)$", message = "同步动作仅支持 ADD、REMOVE 或 ALL")
    private String action;
}
