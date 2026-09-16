package com.ym.agriculture.farming.tenantinit.model.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ym.common.mybatis.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sf_farm_work_dict_template")
public class SfFarmWorkDictTemplate extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId("template_id")
    private Long templateId;

    private String templateCode;
    private String parentTemplateCode;
    private String nodeType;
    private String dictCode;
    private String dictName;
    private Integer minWorkers;
    private Integer maxWorkers;
    private Boolean requiresMaterial;
    private String status;
    private String customFormTemplateJson;
    private Integer sortOrder;
    private String remark;
}
