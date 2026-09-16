package com.ym.agriculture.farming.farmwork.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * stask 农事字典树节点出参。
 *
 * @author ym-cloud
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SfFarmWorkDictTreeVo extends SfFarmWorkDictVo {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 子节点列表。分类下为农事项目；项目节点默认为空列表。
     */
    private List<SfFarmWorkDictTreeVo> children = new ArrayList<>();
}
