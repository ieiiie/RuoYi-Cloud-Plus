package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Map;

/**
 * 农事分类或项目跨服务摘要。
 */
@Data
public class RemoteFarmWorkVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long dictId;
    private Long parentId;
    private String nodeType;
    private String dictName;
    private String dictCode;
    private Integer minWorkers;
    private Integer maxWorkers;
    private Boolean requiresMaterial;
    private String status;
    private Map<String, Object> customFormTemplate;
    private Integer sortOrder;
    private String remark;
}
