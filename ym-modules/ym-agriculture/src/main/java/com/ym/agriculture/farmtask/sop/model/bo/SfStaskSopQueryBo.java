package com.ym.agriculture.farmtask.sop.model.bo;

import lombok.Data;

/** 农事 SOP 分页查询条件。 */
@Data
public class SfStaskSopQueryBo {

    /** 农事项目 ID。 */
    private Long workItemId;

    /** 作物范围：ALL/SPECIFIC。 */
    private String cropScope;

    /** 作物物种 ID。 */
    private Long cropSpeciesId;

    /** 内容语言。 */
    private String language;
}
