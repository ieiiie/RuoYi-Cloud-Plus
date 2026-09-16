package com.ym.agriculture.farmtask.workorder.support;

import cn.hutool.core.collection.CollUtil;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * stask 单次公开查询内的员工批量索引。
 *
 * <p>每个查询显式创建上下文并一次预加载所需员工，后续装配仅查询内存索引。上下文不使用线程变量，
 * 因此不会在线程复用、异步执行或嵌套调用时泄漏缓存。</p>
 */
public final class SfStaskEmployeeQueryContext {

    private final Function<Collection<Long>, List<SysEmployeeVo>> batchLoader;
    private final Set<Long> loadedIds = new HashSet<>();
    private final Map<Long, SysEmployeeVo> employees = new HashMap<>();

    /**
     * 创建员工查询上下文。
     *
     * @param batchLoader 员工批量加载器
     */
    public SfStaskEmployeeQueryContext(Function<Collection<Long>, List<SysEmployeeVo>> batchLoader) {
        this.batchLoader = Objects.requireNonNull(batchLoader, "batchLoader");
    }

    /**
     * 批量预加载尚未查询的员工 ID。
     *
     * @param employeeIds 员工 ID 集合
     */
    public void preload(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return;
        }
        List<Long> missingIds = employeeIds.stream()
            .filter(Objects::nonNull)
            .filter(employeeId -> !loadedIds.contains(employeeId))
            .distinct()
            .toList();
        if (missingIds.isEmpty()) {
            return;
        }
        // 无结果的 ID 也标记为已加载，避免一次业务查询内重复访问员工库。
        loadedIds.addAll(missingIds);
        List<SysEmployeeVo> rows = batchLoader.apply(missingIds);
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        for (SysEmployeeVo employee : rows) {
            if (employee != null && employee.getEmployeeId() != null) {
                employees.put(employee.getEmployeeId(), employee);
            }
        }
    }

    /**
     * 获取当前已加载的只读员工索引。
     *
     * @return 员工 ID 到员工信息的映射
     */
    public Map<Long, SysEmployeeVo> employees() {
        return Map.copyOf(employees);
    }

    /**
     * 从索引查找员工，不触发数据访问。
     *
     * @param employeeId 员工 ID
     * @return 员工信息，不存在时返回 {@code null}
     */
    public SysEmployeeVo find(Long employeeId) {
        return employeeId == null ? null : employees.get(employeeId);
    }

    /**
     * 从索引解析员工姓名，不触发数据访问。
     *
     * @param employeeId 员工 ID
     * @return 员工姓名，不存在时返回 {@code null}
     */
    public String nameOf(Long employeeId) {
        SysEmployeeVo employee = find(employeeId);
        return employee == null ? null : employee.getName();
    }
}
