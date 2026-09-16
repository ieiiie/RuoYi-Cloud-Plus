package com.ym.system.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/** 行政区划树节点。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SysRegionTreeVo extends SysRegionVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<SysRegionTreeVo> children = new ArrayList<>();
}
