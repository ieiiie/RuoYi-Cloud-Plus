package com.ym.iot.jetlinks.support;

import cn.hutool.core.bean.BeanUtil;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.jetlinks.rpc.PageDto;
import com.ym.jetlinks.rpc.QueryDto;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public final class JetLinksMapping {
    public static final Set<String> AUDIT_LONG_FIELDS =
            Set.of(
                    "createBy",
                    "updateBy",
                    "createDept",
                    "operatorId",
                    "openBy",
                    "closeBy",
                    "handleBy");

    private JetLinksMapping() {}

    public static Long id(String id) {
        if (id == null) return null;
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            throw new ServiceException("JetLinks returned a non-Long compatibility ID");
        }
    }

    public static Map<String, Object> data(Object value) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (value != null)
            (value instanceof Map<?, ?> map ? map : BeanUtil.beanToMap(value))
                    .forEach(
                            (key, v) -> {
                                String k = key.toString();
                                if (v != null
                                        && !Set.of(
                                                        "params",
                                                        "searchValue",
                                                        "tenantId",
                                                        "createBy",
                                                        "updateBy",
                                                        "createDept")
                                                .contains(k)) result.put(k, v);
                            });
        return result;
    }

    public static Map<String, Object> filters(Object value, String... whitelist) {
        Map<String, Object> source = data(value), result = new LinkedHashMap<>();
        for (String key : whitelist) if (source.containsKey(key)) result.put(key, source.get(key));
        return result;
    }

    public static RecordDto record(Long id, Object value, long version) {
        return new RecordDto(id == null ? null : id.toString(), data(value), version);
    }

    public static <T> T bean(RecordDto row, String idField, Class<T> type) {
        if (row == null) return null;
        Map<String, Object> data = legacyAudit(row.data());
        if (idField != null) data.put(idField, id(row.id()));
        data.put("version", row.version());
        return BeanUtil.toBean(data, type);
    }

    /** Native user IDs are audit strings, not RuoYi user foreign keys. */
    public static Long numericOperator(Object value) {
        if (value == null || !value.toString().matches("[0-9]{1,19}")) return null;
        try {
            long id = Long.parseLong(value.toString());
            return id > 0 ? id : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Map<String, Object> legacyAudit(Map<String, Object> source) {
        Map<String, Object> data = new LinkedHashMap<>(source);
        boolean nativeCaller =
                source.get("caller") != null && !"ym-iot".equals(source.get("caller"));
        for (String field : AUDIT_LONG_FIELDS)
            if (data.containsKey(field))
                data.put(field, nativeCaller ? null : numericOperator(data.get(field)));
        return data;
    }

    public static <T> List<T> beans(Collection<RecordDto> rows, String idField, Class<T> type) {
        if (rows == null) throw new ServiceException("JetLinks provider returned null records");
        return rows.stream().map(row -> bean(row, idField, type)).toList();
    }

    public static List<String> ids(Collection<Long> ids) {
        if (ids == null) throw new ServiceException("An explicit authorized ID scope is required");
        return ids.stream().filter(Objects::nonNull).distinct().map(String::valueOf).toList();
    }

    public static QueryDto query(List<String> ids, Map<String, Object> filters, PageQuery page) {
        int num = page == null || page.getPageNum() == null ? 1 : Math.max(1, page.getPageNum());
        int size =
                page == null || page.getPageSize() == null
                        ? 500
                        : Math.max(1, Math.min(500, page.getPageSize()));
        String sort = page == null ? null : page.getOrderByColumn();
        String order = page == null ? null : page.getIsAsc();
        if (sort != null && !sort.matches("[a-zA-Z][a-zA-Z0-9,]*"))
            throw new ServiceException("Invalid sort field");
        if ("ascending".equals(order)) order = "asc";
        if ("descending".equals(order)) order = "desc";
        if (order != null && !Set.of("asc", "desc").contains(order))
            throw new ServiceException("Invalid sort direction");
        return new QueryDto(ids, filters, num, size, sort, order);
    }

    /**
     * Exhaust provider pages. A provider that stops early is an error, never a truncated successful
     * list.
     */
    public static List<RecordDto> all(
            Function<QueryDto, CompletableFuture<PageDto<RecordDto>>> fetch, QueryDto q) {
        List<RecordDto> rows = new ArrayList<>();
        for (int page = 1; ; page++) {
            PageDto<RecordDto> result =
                    JetLinksRpcClient.await(
                            fetch.apply(
                                    new QueryDto(
                                            q.ids(), q.filters(), page, 500, q.sort(), q.order())));
            if (result == null || result.records() == null)
                throw new ServiceException("JetLinks provider returned an invalid page");
            rows.addAll(result.records());
            if (rows.size() >= result.total()) return rows;
            if (result.records().isEmpty())
                throw new ServiceException("JetLinks provider truncated pagination");
        }
    }

    public static <T> PageResult<T> page(PageDto<RecordDto> page, String idField, Class<T> type) {
        if (page == null) throw new ServiceException("JetLinks provider returned no page");
        return PageResult.build(beans(page.records(), idField, type), page.total());
    }

    public static <T> PageResult<T> slice(List<T> list, PageQuery page) {
        long size =
                page == null || page.getPageSize() == null
                        ? list.size()
                        : Math.max(1, page.getPageSize());
        long num = page == null || page.getPageNum() == null ? 1 : Math.max(1, page.getPageNum());
        long offset = Math.min(list.size(), (num - 1) * size);
        return PageResult.build(
                list.subList((int) offset, (int) Math.min(list.size(), offset + size)),
                list.size());
    }
}
