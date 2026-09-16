package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasDictData;

import java.io.Serializable;

/**
 * SaaS 字典数据视图。
 */
@Data
@AutoMapper(target = SaasDictData.class)
public class SaasDictDataVo implements Serializable {
    private Long dictCode;
    private Integer dictSort;
    private String dictLabel;
    private String dictValue;
    private String dictType;
    private String cssClass;
    private String listClass;
    private String isDefault;
    private String remark;
}
