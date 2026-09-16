package com.ym.iot.jetlinks.service.support;

import com.alibaba.fastjson2.JSON;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.domain.dto.JetLinksTaskSnapshot;
import com.ym.iot.jetlinks.mapper.JetLinksCommandTaskMapper;
import com.ym.jetlinks.rpc.CommandDto;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Repository
@ConditionalOnJetLinks
public class JetLinksBusinessStore {
    private final JetLinksCommandTaskMapper taskMapper;

    public JetLinksBusinessStore(JetLinksCommandTaskMapper taskMapper) {
        this.taskMapper = taskMapper;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void createTask(
            String requestId,
            Long id,
            String tenant,
            Long version,
            String function,
            Map<String, Object> inputs) {
        taskMapper.createTask(requestId, id, tenant, version, function, JSON.toJSONString(inputs));
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void updateCommand(String requestId, CommandDto command) {
        taskMapper.updateCommand(
                command.commandId(),
                command.state(),
                JSON.toJSONString(command.result()),
                new java.sql.Timestamp(command.updatedAt()),
                requestId,
                new java.sql.Timestamp(command.updatedAt()));
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void markFailed(String requestId, String message) {
        taskMapper.markFailed(message, requestId);
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED,
            timeout = 30,
            rollbackFor = Exception.class)
    public void markUnknown(String requestId, String message) {
        taskMapper.markUnknown(message, requestId);
    }

    public JetLinksTaskSnapshot task(String id) {
        var rows = taskMapper.selectTask(id, id);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    public JetLinksTaskSnapshot activeTask(Long id, String tenant, Long version) {
        var ids = taskMapper.selectActiveTaskIds(id, tenant, version);
        return ids.isEmpty() ? null : task(ids.get(0));
    }
}
