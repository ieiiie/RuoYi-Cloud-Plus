package com.ym.agriculture.farming.integration.ai.algback.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.List;

/**
 * 算法中台分页 {@code PageVo}；{@link #list} 多为行数据，使用 {@link JsonNode} 便于取字段。
 *
 * @author ym-cloud
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AlgBackPageVo {

    /** 当前页码，从 1 起。 */
    private int pageNum;

    /** 每页条数。 */
    private int pageSize;

    /** 总页数。 */
    private int pageCount;

    /** 总记录数。 */
    private long total;

    /** 当前页数据行。 */
    private List<JsonNode> list;
}
