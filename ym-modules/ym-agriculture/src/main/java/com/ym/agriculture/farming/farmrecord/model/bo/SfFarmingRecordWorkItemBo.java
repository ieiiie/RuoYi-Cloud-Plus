package com.ym.agriculture.farming.farmrecord.model.bo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 农事记录选择的农事项目入参。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingRecordWorkItemBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农事项目 ID，对应 sf_farm_work_dict.dict_id，必须为启用 ITEM。
     */
    @NotNull(message = "农事项目 ID 不能为空")
    private Long workItemId;

    /**
     * 记录内排序，越小越靠前；为空时按入参顺序补齐。
     */
    private Integer sortOrder;

    /**
     * 农事项目自定义表单提交值，按模板字段 key 存储。
     */
    private Map<String, Object> customFormData = new LinkedHashMap<>();
}
