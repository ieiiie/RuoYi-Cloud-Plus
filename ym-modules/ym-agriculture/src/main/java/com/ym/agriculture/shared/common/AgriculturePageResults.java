package com.ym.agriculture.shared.common;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ym.common.core.domain.PageResult;

import java.util.Collection;
import java.util.List;

/**
 * 农业业务分页结果构造工具。
 *
 * <p>兼容迁移前业务中“数据库分页”和“内存列表分页”两种语义，
 * 对外统一返回 {@link PageResult}。</p>
 */
public final class AgriculturePageResults {

    private AgriculturePageResults() {
    }

    /**
     * 根据 MyBatis-Plus 分页结果构造响应。
     */
    public static <T> PageResult<T> build(IPage<T> page) {
        if (page == null) {
            return PageResult.build();
        }
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据完整列表构造非分页响应。
     */
    public static <T> PageResult<T> build(Collection<T> rows) {
        return PageResult.build(rows);
    }

    /**
     * 构造空分页响应。
     */
    public static <T> PageResult<T> build() {
        return PageResult.build();
    }

    /**
     * 对完整内存列表执行分页，保持迁移前接口的总数语义。
     */
    public static <T> PageResult<T> build(List<T> rows, IPage<?> page) {
        if (CollUtil.isEmpty(rows)) {
            return PageResult.build();
        }
        int pageNumber = Math.toIntExact(Math.max(page.getCurrent(), 1L));
        int pageSize = Math.toIntExact(Math.max(page.getSize(), 1L));
        return PageResult.build(CollUtil.page(pageNumber - 1, pageSize, rows), rows.size());
    }

}
