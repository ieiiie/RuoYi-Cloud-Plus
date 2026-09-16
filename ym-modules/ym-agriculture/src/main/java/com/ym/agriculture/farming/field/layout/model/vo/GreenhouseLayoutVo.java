package com.ym.agriculture.farming.field.layout.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 大棚二维布局。
 */
@Data
public class GreenhouseLayoutVo {

    private long version;
    private boolean initialized;
    private List<Column> columns;
    private List<Row> unplacedRows;

    @Data
    public static class Column {
        private String columnName;
        private int columnOrder;
        private List<Row> rows;
    }

    @Data
    public static class Row {
        private Long fieldId;
        private String fieldCode;
        private String fieldName;
        private String status;
        private Integer sortOrder;
        private BigDecimal areaMu;
        private int rowOrder;
    }
}
