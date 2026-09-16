package com.ym.agriculture.api.farming.domain.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/** 农业行情品类。 */
@Data
public class RemoteMarketCategoryVo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String category;
    private String categoryName;
    private Integer sort;
}
