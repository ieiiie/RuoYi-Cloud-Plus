package com.ym.agriculture.farming.uav.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.algback.dao.SfAiInferenceLogMapper;
import com.ym.agriculture.farming.uav.dao.SfUavAiTaskMapper;
import com.ym.agriculture.farming.uav.dao.SfUavFlightTaskMapper;
import com.ym.agriculture.farming.uav.dao.SfUavMediaFileMapper;
import com.ym.agriculture.farming.uav.model.bo.SfUavAiTaskQueryBo;
import com.ym.agriculture.farming.uav.model.bo.SfUavFlightTaskQueryBo;
import com.ym.agriculture.farming.uav.model.entity.SfUavAiTask;
import com.ym.agriculture.farming.uav.model.entity.SfUavFlightTask;
import com.ym.agriculture.farming.uav.model.entity.SfUavMediaFile;
import com.ym.agriculture.farming.uav.model.vo.SfUavAiInferenceLogVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavAiTaskVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavFlightTaskVo;
import com.ym.agriculture.farming.uav.model.vo.SfUavMediaFileVo;
import com.ym.agriculture.farming.uav.service.IUavLocalTaskQueryService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 本地飞行任务 / AI 任务只读查询实现。
 */
@RequiredArgsConstructor
@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class UavLocalTaskQueryServiceImpl implements IUavLocalTaskQueryService {

    private final SfUavFlightTaskMapper flightTaskMapper;
    private final SfUavAiTaskMapper aiTaskMapper;
    private final SfAiInferenceLogMapper aiInferenceLogMapper;
    private final SfUavMediaFileMapper mediaFileMapper;

    @Override
    public PageResult<SfUavFlightTaskVo> pageFlightTasks(SfUavFlightTaskQueryBo bo, PageQuery pageQuery) {
        List<Long> allowedFieldIds = resolveAllowedFieldIds();
        if (allowedFieldIds != null && allowedFieldIds.isEmpty()) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build();
        }
        if (bo != null && bo.getFieldId() != null) {
            if (allowedFieldIds != null && !allowedFieldIds.contains(bo.getFieldId())) {
                return com.ym.agriculture.shared.common.AgriculturePageResults.build();
            }
        }
        var w = flightTaskMapper.buildLocalPageWrapper(bo, allowedFieldIds);
        w.eq(SfUavFlightTask::getTenantId, requireTenant());
        w.orderByDesc(SfUavFlightTask::getCreateTime);
        Page<SfUavFlightTask> page = flightTaskMapper.selectPage(pageQuery.build(), w);
        List<SfUavFlightTaskVo> rows = page.getRecords().stream()
            .map(e -> MapstructUtils.convert(e, SfUavFlightTaskVo.class))
            .toList();
        Page<SfUavFlightTaskVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(rows);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(voPage);
    }

    @Override
    public SfUavFlightTaskVo getFlightTask(Long id) {
        if (id == null) {
            return null;
        }
        SfUavFlightTask row = flightTaskMapper.selectById(id);
        if (row == null) {
            return null;
        }
        assertHistoricalTenant(row.getTenantId());
        SfUavFlightTaskVo vo = MapstructUtils.convert(row, SfUavFlightTaskVo.class);
        vo.setMediaFiles(mediaFileMapper.selectBySyncJobId(row.getTenantId(), row.getUavJobId()).stream()
            .map(media -> MapstructUtils.convert(media, SfUavMediaFileVo.class)).toList());
        return vo;
    }

    @Override
    public PageResult<SfUavAiTaskVo> pageAiTasks(SfUavAiTaskQueryBo bo, PageQuery pageQuery) {
        List<Long> allowedFieldIds = resolveAllowedFieldIds();
        if (allowedFieldIds != null && allowedFieldIds.isEmpty()) {
            return com.ym.agriculture.shared.common.AgriculturePageResults.build();
        }
        if (bo != null && bo.getFieldId() != null) {
            if (allowedFieldIds != null && !allowedFieldIds.contains(bo.getFieldId())) {
                return com.ym.agriculture.shared.common.AgriculturePageResults.build();
            }
        }
        var w = aiTaskMapper.buildLocalPageWrapper(bo, allowedFieldIds);
        w.eq(SfUavAiTask::getTenantId, requireTenant());
        w.orderByDesc(SfUavAiTask::getCreateTime);
        Page<SfUavAiTask> page = aiTaskMapper.selectPage(pageQuery.build(), w);
        List<SfUavAiTaskVo> rows = page.getRecords().stream()
            .map(e -> MapstructUtils.convert(e, SfUavAiTaskVo.class))
            .toList();
        Page<SfUavAiTaskVo> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(rows);
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(voPage);
    }

    @Override
    public SfUavAiTaskVo getAiTask(Long id) {
        if (id == null) {
            return null;
        }
        SfUavAiTask row = aiTaskMapper.selectById(id);
        if (row == null) {
            return null;
        }
        assertHistoricalTenant(row.getTenantId());
        return buildAiTaskDetailVo(row);
    }

    @Override
    public SfUavAiTaskVo getAiTaskByUavJobId(String uavJobId) {
        if (StringUtils.isBlank(uavJobId)) {
            return null;
        }
        List<Long> allowedFieldIds = resolveAllowedFieldIds();
        if (allowedFieldIds != null && allowedFieldIds.isEmpty()) {
            return null;
        }
        SfUavAiTask row = aiTaskMapper.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers.<SfUavAiTask>lambdaQuery()
            .eq(SfUavAiTask::getTenantId, requireTenant())
            .eq(SfUavAiTask::getUavJobId, uavJobId.trim())
            .orderByDesc(SfUavAiTask::getCreateTime).orderByDesc(SfUavAiTask::getId).last("LIMIT 1"));
        if (row == null) {
            return null;
        }
        assertHistoricalTenant(row.getTenantId());
        return buildAiTaskDetailVo(row);
    }

    private SfUavAiTaskVo buildAiTaskDetailVo(SfUavAiTask row) {
        SfUavAiTaskVo vo = MapstructUtils.convert(row, SfUavAiTaskVo.class);
        vo.setInferenceLogs(loadInferenceLogsForAiTask(row));
        return vo;
    }

    private List<SfUavAiInferenceLogVo> loadInferenceLogsForAiTask(SfUavAiTask task) {
        if (task == null || StringUtils.isBlank(task.getAlgTaskNo()) || StringUtils.isBlank(task.getTenantId())) {
            return Collections.emptyList();
        }
        List<SfUavAiInferenceLogVo> logs = aiInferenceLogMapper
            .selectListForAiTaskDetail(task.getAlgTaskNo().trim(), task.getTenantId(), 5000)
            .stream()
            .map(e -> {
                SfUavAiInferenceLogVo vo = MapstructUtils.convert(e, SfUavAiInferenceLogVo.class);
                if (vo != null) {
                    vo.setLat(e.getShootLat());
                    vo.setLng(e.getShootLng());
                }
                return vo;
            })
            .toList();
        if (logs.isEmpty()) {
            return logs;
        }
        enrichWithMediaGeo(logs, task.getTenantId(), task.getUavJobId());
        return logs;
    }

    /**
     * 通过 {@code sf_ai_inference_log.task_name} 与 {@code sf_uav_media_file.file_name} 匹配，填充拍摄经纬度和时间。
     */
    private void enrichWithMediaGeo(List<SfUavAiInferenceLogVo> logs, String tenantId, String uavJobId) {
        if (StringUtils.isBlank(uavJobId)) {
            return;
        }
        List<SfUavMediaFile> mediaFiles = mediaFileMapper.selectBySyncJobId(tenantId, uavJobId);
        if (mediaFiles.isEmpty()) {
            return;
        }
        Map<Long, SfUavMediaFile> byId = mediaFiles.stream()
            .filter(m -> m.getUavMediaId() != null)
            .collect(Collectors.toMap(SfUavMediaFile::getUavMediaId, m -> m, (a, b) -> a));
        Map<String, SfUavMediaFile> byFileId = mediaFiles.stream()
            .filter(m -> StringUtils.isNotBlank(m.getFileId()))
            .collect(Collectors.toMap(m -> m.getFileId().trim(), m -> m, (a, b) -> a));
        Map<String, SfUavMediaFile> byObjectKey = mediaFiles.stream()
            .filter(m -> StringUtils.isNotBlank(m.getObjectKey()))
            .collect(Collectors.toMap(m -> m.getObjectKey().trim(), m -> m, (a, b) -> a));
        Map<String, SfUavMediaFile> byFileName = mediaFiles.stream()
            .filter(m -> StringUtils.isNotBlank(m.getFileName()))
            .collect(Collectors.toMap(m -> m.getFileName().trim(), m -> m, (a, b) -> a));
        for (SfUavAiInferenceLogVo vo : logs) {
            if (hasCompleteGeo(vo)) {
                continue;
            }
            SfUavMediaFile mf = matchMedia(vo, byId, byFileId, byObjectKey, byFileName);
            if (mf == null) {
                mf = fuzzyMatchByTaskName(vo, mediaFiles);
            }
            if (mf != null) {
                applyMissingGeo(vo, mf);
            }
        }
    }

    private static boolean hasCompleteGeo(SfUavAiInferenceLogVo vo) {
        return vo != null
            && StringUtils.isNotBlank(vo.getLat())
            && StringUtils.isNotBlank(vo.getLng())
            && vo.getShootTime() != null;
    }

    private static SfUavMediaFile matchMedia(SfUavAiInferenceLogVo vo,
                                             Map<Long, SfUavMediaFile> byId,
                                             Map<String, SfUavMediaFile> byFileId,
                                             Map<String, SfUavMediaFile> byObjectKey,
                                             Map<String, SfUavMediaFile> byFileName) {
        if (vo == null) {
            return null;
        }
        if (vo.getUavMediaId() != null) {
            SfUavMediaFile media = byId.get(vo.getUavMediaId());
            if (media != null) {
                return media;
            }
        }
        SfUavMediaFile media = getByTrimmedKey(byFileId, vo.getSourceFileId());
        if (media != null) {
            return media;
        }
        media = getByTrimmedKey(byObjectKey, vo.getSourceObjectKey());
        if (media != null) {
            return media;
        }
        media = getByTrimmedKey(byFileName, vo.getSourceFileName());
        if (media != null) {
            return media;
        }
        return getByTrimmedKey(byFileName, vo.getTaskName());
    }

    private static SfUavMediaFile getByTrimmedKey(Map<String, SfUavMediaFile> index, String key) {
        if (index == null || StringUtils.isBlank(key)) {
            return null;
        }
        return index.get(key.trim());
    }

    private static SfUavMediaFile fuzzyMatchByTaskName(SfUavAiInferenceLogVo vo, List<SfUavMediaFile> mediaFiles) {
        if (vo == null || StringUtils.isBlank(vo.getTaskName())) {
            return null;
        }
        String taskName = vo.getTaskName().trim();
        for (SfUavMediaFile m : mediaFiles) {
            if (StringUtils.isBlank(m.getFileName())) {
                continue;
            }
            String fn = m.getFileName().trim();
            if (taskName.equals(fn) || taskName.endsWith(fn) || fn.endsWith(taskName)) {
                return m;
            }
        }
        return null;
    }

    private static void applyMissingGeo(SfUavAiInferenceLogVo vo, SfUavMediaFile mf) {
        if (StringUtils.isBlank(vo.getLat())) {
            vo.setLat(mf.getLat());
        }
        if (StringUtils.isBlank(vo.getLng())) {
            vo.setLng(mf.getLng());
        }
        if (vo.getShootTime() == null) {
            vo.setShootTime(mf.getFileCreateTime());
        }
    }

    /**
     * 当前已暂时移除"按操作人限制"，统一返回 {@code null}（不限制地块）；原"非超管按本人负责地块"逻辑块注释保留备查。
     */






    private List<Long> resolveAllowedFieldIds() {
        requireTenant();
        // Historical access uses the record's original tenant, never current device/field assignment.
        return null;
    }

    private String requireTenant() {
        if (LoginHelper.getLoginUser() == null || StringUtils.isBlank(LoginHelper.getLoginUser().getTenantId())) {
            throw new ServiceException("当前登录缺少租户上下文");
        }
        return LoginHelper.getLoginUser().getTenantId();
    }

    private void assertHistoricalTenant(String tenantId) {
        if (!requireTenant().equals(tenantId)) throw new ServiceException("无权限访问该历史记录");
    }
}
