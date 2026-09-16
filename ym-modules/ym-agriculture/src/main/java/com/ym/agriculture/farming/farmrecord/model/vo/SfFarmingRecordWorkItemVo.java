package com.ym.agriculture.farming.farmrecord.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 农事记录项目明细快照出参。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingRecordWorkItemVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事项目 ID。
     */
    private Long workItemId;

    /**
     * 农事项目编码快照。
     */
    private String workItemCode;

    /**
     * 农事项目名称快照。
     */
    private String workItemName;

    /**
     * 农事分类 ID。
     */
    private Long categoryId;

    /**
     * 农事分类编码快照。
     */
    private String categoryCode;

    /**
     * 农事分类名称快照。
     */
    private String categoryName;

    /**
     * 农事项目自定义表单模板快照。
     */
    private Object customFormTemplate;

    /**
     * 农事项目自定义表单提交值。
     */
    private Object customFormData;

    /**
     * 记录内排序，越小越靠前。
     */
    private Integer sortOrder;
}
