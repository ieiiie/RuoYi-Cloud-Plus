package com.ym.system.saas.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/** SaaS 行政区划树节点。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SaasRegionTreeVo extends SaasRegionVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<SaasRegionTreeVo> children = new ArrayList<>();
}
