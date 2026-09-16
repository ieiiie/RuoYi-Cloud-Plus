package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ym.agriculture.farmtask.workorder.model.bo.SfStaskTaskScopeBo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * stask 任务范围（大棚 + 农事项目）SQL 条件辅助。
 */
public final class StaskTaskScopeSqlHelper {

    private StaskTaskScopeSqlHelper() {
    }

    /**
     * 为查询条件追加 (greenhouse_id, work_item_id) 组合匹配。
     *
     * @param wrapper           查询包装器
     * @param scopes            任务范围列表
     * @param greenhouseColumn  大棚字段名（含表别名）
     * @param workItemColumn    农事项目字段名（含表别名）
     */
    public static void applyScopePairMatch(LambdaQueryWrapper<?> wrapper, Collection<SfStaskTaskScopeBo> scopes,
        String greenhouseColumn, String workItemColumn) {
        if (wrapper == null || CollUtil.isEmpty(scopes)) {
            return;
        }
        StringBuilder sql = new StringBuilder("(");
        List<Object> params = new ArrayList<>(scopes.size() * 2);
        int index = 0;
        for (SfStaskTaskScopeBo scope : scopes) {
            if (scope == null || scope.getGreenhouseId() == null || scope.getWorkItemId() == null) {
                continue;
            }
            if (index > 0) {
                sql.append(" OR ");
            }
            sql.append("(").append(greenhouseColumn).append(" = {").append(index)
                .append("} AND ").append(workItemColumn).append(" = {").append(index + 1).append("})");
            params.add(scope.getGreenhouseId());
            params.add(scope.getWorkItemId());
            index += 2;
        }
        if (params.isEmpty()) {
            return;
        }
        sql.append(")");
        wrapper.apply(sql.toString(), params.toArray());
    }

    /**
     * 将状态集合转为 SQL IN 字面量（仅用于服务端常量状态，禁止传入用户输入）。
     *
     * @param statuses 状态集合
     * @return IN 子句内容
     */
    public static String toSqlInLiterals(Collection<String> statuses) {
        if (CollUtil.isEmpty(statuses)) {
            return "''";
        }
        return statuses.stream()
            .map(status -> "'" + status.replace("'", "''") + "'")
            .collect(Collectors.joining(","));
    }
}
