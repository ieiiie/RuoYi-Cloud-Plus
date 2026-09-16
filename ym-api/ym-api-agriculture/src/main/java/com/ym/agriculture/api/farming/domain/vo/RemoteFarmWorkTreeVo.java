package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * 农事分类与已启用项目树。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemoteFarmWorkTreeVo extends RemoteFarmWorkVo {

    @Serial
    private static final long serialVersionUID = 1L;

    private List<RemoteFarmWorkTreeVo> children = new ArrayList<>();
}
