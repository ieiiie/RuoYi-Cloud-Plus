package com.ym.agriculture.farmtask.sop.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/** 农事 SOP 后台详情与列表出参。 */
@Data
public class SfStaskSopVo {

    /** SOP 主键。 */
    private Long sopId;

    /** 农事项目 ID。 */
    private Long workItemId;

    /** 农事项目名称。 */
    private String workItemName;

    /** 作物范围：ALL/SPECIFIC。 */
    private String cropScope;

    /** 作物物种 ID；全部作物时为空。 */
    private Long cropSpeciesId;

    /** 作物物种名称；全部作物时为全部。 */
    private String cropSpeciesName;

    /** 内容语言。 */
    private String language;

    /** 有序内容块；列表调用可为空。 */
    private List<SfStaskSopContentBlockVo> contentBlocks;

    /** 创建时间。 */
    private Date createTime;

    /** 最后编辑时间。 */
    private Date updateTime;
}
