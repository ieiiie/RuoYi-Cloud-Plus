package com.ym.agriculture.api.farmtask.domain.bo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/** 完工或重新申请验收命令。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteTaskCompleteBo extends RemoteTaskCommandBo {
    @Serial
    private static final long serialVersionUID = 1L;
    private String workPhotos;
    private String completionRemark;
}
