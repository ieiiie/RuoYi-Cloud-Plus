package com.ym.agriculture.api.farming.domain.bo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/** 农业行情跨服务分页查询条件。 */
@Data
public class RemoteMarketQuoteQueryBo implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String keyword;
    private String category;
    private LocalDate quoteDate;
    private Integer pageNum;
    private Integer pageSize;
}
