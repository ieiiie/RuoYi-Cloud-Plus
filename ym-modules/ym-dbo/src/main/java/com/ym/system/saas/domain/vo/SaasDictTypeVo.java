package com.ym.system.saas.domain.vo;

import io.github.linpeilie.annotations.AutoMapper;
import lombok.Data;
import com.ym.system.saas.domain.SaasDictType;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SaaS 字典类型视图。
 */
@Data
@AutoMapper(target = SaasDictType.class)
public class SaasDictTypeVo implements Serializable {
    private Long dictId;
    private String dictName;
    private String dictType;
    private String remark;
    private LocalDateTime createTime;
}
