package com.ym.agriculture.farming.field.layout.model.bo;

import lombok.Data;

import java.util.List;

/**
 * 大棚布局草稿。
 */
@Data
public class GreenhouseLayoutDraftBo {

    private Long version;
    private List<Column> columns;

    @Data
    public static class Column {
        private String columnName;
        private List<Item> items;
    }

    @Data
    public static class Item {
        private Long fieldId;
        private Boolean currentField;
    }
}
