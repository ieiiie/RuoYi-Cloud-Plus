package com.ym.agriculture.farmtask.inspectionphotoarchive.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/** 小程序列表页的大棚布局分组。 */
@Data
public class SfInspectionPhotoArchiveMiniappGroupVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 分组名称，未配置布局的大棚归入“未分组”。 */
    private String groupName;

    /** 分组展示顺序。 */
    private Integer groupOrder;

    /** 当前分组下的大棚。 */
    private List<SfInspectionPhotoArchiveMiniappFieldVo> fields;
}
