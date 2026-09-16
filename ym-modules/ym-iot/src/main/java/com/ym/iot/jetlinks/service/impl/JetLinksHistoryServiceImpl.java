package com.ym.iot.jetlinks.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.iot.fertilizer.domain.IotFertilizerRecord;
import com.ym.iot.fertilizer.domain.bo.IotFertilizerRecordBo;
import com.ym.iot.fertilizer.domain.vo.IotFertilizerRecordVo;
import com.ym.iot.fertilizer.mapper.IotFertilizerRecordMapper;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksHistoryService;
import com.ym.iot.motorvalve.domain.ValveControlLog;
import com.ym.iot.motorvalve.domain.bo.ValveControlLogBo;
import com.ym.iot.motorvalve.domain.bo.ValveSessionBo;
import com.ym.iot.motorvalve.domain.vo.ValveControlLogVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionStatsVo;
import com.ym.iot.motorvalve.domain.vo.ValveSessionVo;
import com.ym.iot.motorvalve.mapper.ValveControlLogMapper;
import com.ym.iot.motorvalve.mapper.ValveSessionMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.*;

/** 按实际登录租户查询业务历史，设备转移后原记录仍属于发生时的租户。 */
@Service
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksHistoryServiceImpl implements IJetLinksHistoryService {
    private final IotFertilizerRecordMapper fertilizerMapper;
    private final ValveControlLogMapper controlLogMapper;
    private final ValveSessionMapper sessionMapper;

    public static String tenant() {
        String tenant = TenantHelper.getTenantId();
        if (!LoginHelper.isLogin() || tenant == null || tenant.isBlank())
            throw new ServiceException("历史记录查询需要已认证的实际租户");
        return tenant;
    }

    public PageResult<IotFertilizerRecordVo> fertilizer(IotFertilizerRecordBo bo, PageQuery query) {
        Page<IotFertilizerRecord> page = historyPage(query);
        long requested = page.getCurrent();
        return result(fertilizerMapper.selectHistoryPage(tenant(), bo, page), requested);
    }

    public IotFertilizerRecordVo fertilizer(Long recordId) {
        return fertilizerMapper.selectHistoryById(tenant(), recordId);
    }

    public PageResult<ValveControlLogVo> valveLogs(ValveControlLogBo bo, PageQuery query) {
        Page<ValveControlLog> page = historyPage(query);
        long requested = page.getCurrent();
        return result(controlLogMapper.selectHistoryPage(tenant(), bo, page), requested);
    }

    private static <T> Page<T> historyPage(PageQuery query) {
        long size =
                query == null || query.getPageSize() == null
                        ? 20
                        : Math.max(1, query.getPageSize());
        long number =
                query == null || query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        // 历史列表默认限量，防止缺少分页参数时一次加载全部业务历史。
        return new Page<>(number, Math.min(size, 500));
    }

    private static <T> PageResult<T> result(IPage<T> page, long requested) {
        // 框架可能把越界页重置为第一页；历史接口继续返回空页，不重复展示第一页。
        return PageResult.build(
                page.getCurrent() == requested ? page.getRecords() : List.of(), page.getTotal());
    }

    public List<ValveSessionVo> sessions(ValveSessionBo bo) {
        var rows = sessionMapper.selectHistory(tenant(), bo);
        for (var row : rows)
            if ("OPEN".equals(row.getStatus()) && row.getOpenTime() != null)
                row.setDurationSeconds(
                        (int)
                                Math.min(
                                        Integer.MAX_VALUE,
                                        Math.max(
                                                0,
                                                (System.currentTimeMillis()
                                                                - row.getOpenTime().getTime())
                                                        / 1000)));
        return rows;
    }

    public List<ValveSessionVo> recentSessions(int limit) {
        return sessionMapper.selectRecentHistory(tenant(), limit);
    }

    public ValveSessionStatsVo stats(ValveSessionBo bo) {
        return sessionMapper.selectHistoryStats(tenant(), bo);
    }
}
