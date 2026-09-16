package com.ym.agriculture.farming.farmrecord.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 移动端农事表单字典选项。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingDictOptionVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 字典键值，对应 sys_dict_data.dict_value。
     */
    private String value;

    /**
     * 字典标签，对应 sys_dict_data.dict_label。
     */
    private String label;

    /**
     * 排序号，越小越靠前。
     */
    private Integer sort;

    /**
     * 是否默认：Y-是，N-否。
     */
    private String defaultFlag;
}
