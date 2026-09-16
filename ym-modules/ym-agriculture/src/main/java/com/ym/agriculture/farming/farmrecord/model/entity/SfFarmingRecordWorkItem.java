package com.ym.agriculture.farming.farmrecord.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 农事记录项目明细，对应 {@code sf_farming_record_work_item}。
 *
 * @author ym-cloud
 */
@Data
@TableName("sf_farming_record_work_item")
public class SfFarmingRecordWorkItem implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 记录农事项目明细主键。
     */
    @TableId("item_id")
    private Long itemId;

    /**
     * 租户编号。
     */
    private String tenantId;

    /**
     * 农事记录 ID。
     */
    private Long recordId;

    /**
     * 农事项目 ID，对应 sf_farm_work_dict.dict_id。
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
     * 农事项目自定义表单模板快照 JSON。
     */
    private String customFormTemplateJson;

    /**
     * 农事项目自定义表单提交值 JSON。
     */
    private String customFormDataJson;

    /**
     * 记录内排序，越小越靠前。
     */
    private Integer sortOrder;
}
