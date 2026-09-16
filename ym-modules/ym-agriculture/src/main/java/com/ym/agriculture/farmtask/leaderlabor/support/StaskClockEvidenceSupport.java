package com.ym.agriculture.farmtask.leaderlabor.support;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.ym.common.core.exception.ServiceException;
import com.ym.agriculture.farmtask.clocklocation.model.vo.SfStaskClockLocationVo;
import com.ym.agriculture.farmtask.clocklocation.support.StaskClockDistanceHelper;
import com.ym.agriculture.farmtask.clocklocation.support.StaskClockFenceHelper;
import com.ym.agriculture.shared.i18n.StaskErrorCodes;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;

import java.math.BigDecimal;

/** 单条、批量到岗打卡共用的凭证与围栏校验。 */
public final class StaskClockEvidenceSupport {

    private StaskClockEvidenceSupport() {
    }

    public static ClockEvidence validate(String clockType, BigDecimal longitude, BigDecimal latitude,
        String proofPhotos, SfStaskClockLocationVo location, StaskMessageResolver messages) {
        if ("GPS".equalsIgnoreCase(clockType)) {
            boolean requireFence = StaskClockFenceHelper.isFenceCheckRequired(location);
            if (requireFence && (longitude == null || latitude == null)) {
                throw messages.stableException(StaskErrorCodes.CLOCK_LOCATION_REQUIRED,
                    StaskMessageKeys.ERROR_EXECUTION_EVIDENCE_REQUIRED);
            }
            Integer distance = StaskClockDistanceHelper.computeDistanceMeters(longitude, latitude, location);
            if (requireFence && (distance == null || distance > location.getRadiusMeters())) {
                throw messages.stableException(StaskErrorCodes.CLOCK_OUT_OF_FENCE,
                    StaskMessageKeys.ERROR_EXECUTION_EVIDENCE_REQUIRED);
            }
            return new ClockEvidence("GPS", longitude, latitude, null, distance);
        }
        if (!"PHOTO".equalsIgnoreCase(clockType)) {
            throw messages.exception(StaskMessageKeys.ERROR_EXECUTION_CLOCK_TYPE_REQUIRED);
        }
        if (StrUtil.isBlank(proofPhotos)) {
            throw messages.stableException(StaskErrorCodes.CLOCK_PHOTO_INVALID,
                StaskMessageKeys.ERROR_EXECUTION_EVIDENCE_REQUIRED);
        }
        try {
            JSONArray photos = JSON.parseArray(proofPhotos);
            if (photos.size() < 1 || photos.size() > 6) {
                throw messages.stableException(StaskErrorCodes.CLOCK_PHOTO_INVALID,
                    StaskMessageKeys.ERROR_EXECUTION_EVIDENCE_REQUIRED);
            }
            return new ClockEvidence("PHOTO", null, null, JSON.toJSONString(photos), null);
        } catch (JSONException exception) {
            throw messages.stableException(StaskErrorCodes.CLOCK_PHOTO_INVALID,
                StaskMessageKeys.ERROR_FIELD_JSON_ARRAY);
        }
    }

    /** 已校验、可持久化的共用打卡凭证。 */
    public record ClockEvidence(String clockType, BigDecimal longitude, BigDecimal latitude,
                                String proofPhotos, Integer distanceMeters) {
    }
}
