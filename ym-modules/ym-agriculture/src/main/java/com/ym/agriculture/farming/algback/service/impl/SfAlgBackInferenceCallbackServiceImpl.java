package com.ym.agriculture.farming.algback.service.impl;

import cn.hutool.core.lang.Dict;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.algback.dao.SfAiInferenceLogMapper;
import com.ym.agriculture.farming.algback.dao.SfAlgBackCustomerBindingMapper;
import com.ym.agriculture.farming.algback.model.dto.AlgBackInferencePushDto;
import com.ym.agriculture.farming.algback.model.entity.SfAiInferenceLog;
import com.ym.agriculture.farming.algback.model.entity.SfAlgBackCustomerBinding;
import com.ym.common.json.utils.JsonUtils;
import com.ym.agriculture.farming.algback.service.ISfAlgBackInferenceCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 按 {@code customerNo} 解析租户后写入 {@code sf_ai_inference_log}；幂等依赖库唯一键。
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SfAlgBackInferenceCallbackServiceImpl implements ISfAlgBackInferenceCallbackService {

    private final SfAlgBackCustomerBindingMapper bindingMapper;
    private final SfAiInferenceLogMapper inferenceLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleInferencePush(AlgBackInferencePushDto dto, String rawJson) {
        if (dto == null || StringUtils.isBlank(dto.getCustomerNo())) {
            log.warn("算法中台回调缺少 customerNo，已忽略");
            return;
        }
        if (StringUtils.isBlank(dto.getTaskNo())) {
            log.warn("算法中台回调缺少 taskNo，已忽略");
            return;
        }
        SfAlgBackCustomerBinding binding = bindingMapper.selectByCustomerNoIgnoreTenant(dto.getCustomerNo().trim());
        if (binding == null) {
            log.warn("算法中台回调 customerNo={} 未找到绑定，已忽略", dto.getCustomerNo());
            return;
        }
        String tenantId = binding.getTenantId();
        TenantHelper.dynamic(tenantId, () -> insertLogRow(tenantId, dto, rawJson));
    }

    private void insertLogRow(String tenantId, AlgBackInferencePushDto dto, String rawJson) {
        SfAiInferenceLog row = new SfAiInferenceLog();
        row.setTenantId(tenantId);
        row.setTaskNo(dto.getTaskNo().trim());
        row.setTaskName(dto.getTaskName());
        row.setModelNo(dto.getModelNo());
        row.setModelName(dto.getModelName());
        row.setCustomerNo(dto.getCustomerNo());
        row.setCustomerName(dto.getCustomerName());
        row.setAlgorithmTypeId(dto.getAlgorithmTypeId());
        row.setAlgorithmTypeValue(dto.getAlgorithmTypeValue());
        // clsScore：直接存储中台推送的原始字符串，并尝试结合模型编码解析出标签与数值
        if (StringUtils.isNotBlank(dto.getClsScore())) {
            String rawScore = dto.getClsScore().trim();
            row.setClsScore(rawScore);
            try {
                String jsonLike = rawScore.replace('\'', '"');
                Dict dict = JsonUtils.parseMap(jsonLike);
                if (dict != null && !dict.isEmpty()) {
                    var entry = dict.entrySet().iterator().next();
                    String dictValue = entry.getKey();
                    Object scoreObj = entry.getValue();
                    String scoreStr = scoreObj != null ? String.valueOf(scoreObj) : null;
                    row.setClsScoreLabel(dictValue);
                    row.setClsScoreValue(scoreStr);
                }
            } catch (Exception ex) {
                log.warn("算法中台回调 clsScore 解析失败, 原始值={}", rawScore, ex);
            }
        }
        row.setImgUrl(StringUtils.isNotBlank(dto.getImgUrl()) ? dto.getImgUrl() : "");
        // alarmTime：字符串 → Long（优先按时间戳解析，失败则按 yyyy-MM-dd HH:mm:ss 解析）
        Long alarmMillis = null;
        if (StringUtils.isNotBlank(dto.getAlarmTime())) {
            try {
                alarmMillis = Long.parseLong(dto.getAlarmTime());
            } catch (NumberFormatException ex) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(
                        dto.getAlarmTime(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    );
                    alarmMillis = ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                } catch (Exception e) {
                    log.warn("算法中台回调 alarmTime 解析失败, 原始值={}", dto.getAlarmTime(), e);
                }
            }
        }
        row.setAlarmTime(alarmMillis);
        row.setVideoPlayUrl(dto.getVideoPlayUrl());
        row.setStreamServerUrl(dto.getStreamServerUrl());
        row.setComputingVideoPlayUrl(dto.getComputingVideoPlayUrl());
        row.setPushVideoPlayUrl(dto.getPushVideoPlayUrl());
        row.setRawJson(rawJson);
        row.setCreateTime(java.time.LocalDateTime.now());
        try {
            inferenceLogMapper.insert(row);
        } catch (DataIntegrityViolationException e) {
            log.debug("算法中台回调幂等跳过: tenantId={} taskNo={}", tenantId, dto.getTaskNo());
        }
    }
}
