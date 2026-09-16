package com.ym.agriculture.farmtask.screen.model.vo;

import lombok.Data;

import java.util.List;

/** 大屏简化分页对象。 */
@Data
public class ScreenPageVo<T> {
    private long total;
    private List<T> rows;

    public static <T> ScreenPageVo<T> of(long total, List<T> rows) {
        ScreenPageVo<T> page = new ScreenPageVo<>();
        page.setTotal(total);
        page.setRows(rows);
        return page;
    }
}
