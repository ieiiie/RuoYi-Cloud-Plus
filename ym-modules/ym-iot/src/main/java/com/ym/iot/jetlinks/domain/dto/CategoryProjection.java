package com.ym.iot.jetlinks.domain.dto;

/** JetLinks 分类事件的持久化参数，源版本控制在投影事务内完成。 */
public record CategoryProjection(
        String categoryId,
        String code,
        String name,
        String parentId,
        long sortIndex,
        int archived,
        long sourceTime,
        long version) {}
