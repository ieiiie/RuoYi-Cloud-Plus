package com.ym.agriculture.farming.farmrecord.model.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 移动端新增农事页面基础选项集合。
 *
 * @author ym-cloud
 */
@Data
public class SfFarmingOptionsVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 农机类型选项，来源：sys_dict_data / sf_farming_machine_type。
     */
    private List<SfFarmingDictOptionVo> machineTypes = new ArrayList<>();

    /**
     * 物料名称选项，来源：sys_dict_data / sf_farming_material_name。
     */
    private List<SfFarmingDictOptionVo> materials = new ArrayList<>();
}
