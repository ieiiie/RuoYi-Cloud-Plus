package com.ym.agriculture.farmtask.workorder.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.BilingualContent;
import com.ym.agriculture.shared.i18n.StaskBilingualMessageFormatter;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskDispatch;
import com.ym.agriculture.farmtask.workorder.service.ISfStaskWorkerInviteSmsService;
import com.ym.agriculture.farmtask.workorder.support.SfStaskSmsNotifySender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工人派工邀请短信历史实现。
 *
 * <p>派工流程已废弃，保留实现类和接口以兼容既有定时任务配置，但不再发送短信。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Deprecated
public class SfStaskWorkerInviteSmsServiceImpl implements ISfStaskWorkerInviteSmsService {

    private static final String SCENE_WORKER_INVITE_BATCH = "WORKER_INVITE_BATCH";

    private final SfStaskDispatchMapper dispatchMapper;
    private final ObjectProvider<SfStaskSmsNotifySender> smsNotifySenderProvider;
    private final StaskBilingualMessageFormatter bilingualMessageFormatter;

    @Override
    public void flushPendingInvites() {
        log.info("工人派工邀请短信功能已废弃，不再发送待确认邀请");
    }

    private void flushPendingInvitesInternal(SfStaskSmsNotifySender smsNotifySender) {
        List<SfStaskDispatch> pendingRows = dispatchMapper.selectPendingInviteSmsDispatches();
        if (CollUtil.isEmpty(pendingRows)) {
            log.debug("工人派工邀请短信：无待发送记录");
            return;
        }
        Map<String, List<SfStaskDispatch>> grouped = new LinkedHashMap<>();
        for (SfStaskDispatch row : pendingRows) {
            if (row.getTenantId() == null || row.getWorkerId() == null) {
                continue;
            }
            String key = row.getTenantId() + ":" + row.getWorkerId();
            grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        int sentGroups = 0;
        int markedRows = 0;
        for (List<SfStaskDispatch> group : grouped.values()) {
            if (CollUtil.isEmpty(group)) {
                continue;
            }
            SfStaskDispatch first = group.get(0);
            String tenantId = first.getTenantId();
            Long workerId = first.getWorkerId();
            List<Long> dispatchIds = group.stream()
                .map(SfStaskDispatch::getDispatchId)
                .filter(id -> id != null)
                .toList();
            if (dispatchIds.isEmpty()) {
                continue;
            }
            BilingualContent content = bilingualMessageFormatter.format(dispatchIds.size() <= 1
                ? StaskMessageKeys.NOTIFY_WORKER_INVITE : StaskMessageKeys.NOTIFY_WORKER_INVITE_BATCH,
                dispatchIds.size());
            boolean success = smsNotifySender.send(SCENE_WORKER_INVITE_BATCH, workerId, content);
            if (!success) {
                log.warn("工人派工邀请短信发送失败 tenantId={}, workerId={}, count={}", tenantId, workerId, dispatchIds.size());
                continue;
            }
            Date sentAt = new Date();
            int updated = dispatchMapper.markInviteSmsSent(tenantId, dispatchIds, sentAt);
            sentGroups++;
            markedRows += updated;
        }
        log.info("工人派工邀请短信批量发送完成，分组数={}，标记行数={}", sentGroups, markedRows);
    }

}
