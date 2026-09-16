package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 小程序作物品种选项。
 */
@Data
public class RemoteCropVarietySimpleVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long speciesId;
    private String speciesName;
    private Long varietyId;
    private String varietyName;
}
