package com.ym.agriculture.farming.trace.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.trace.model.vo.TraceCertificationVo;

import java.util.Collections;
import java.util.List;

/**
 * 认证 JSON 解析。
 */
public final class TraceCertificationSupport {

    private TraceCertificationSupport() {
    }

    public static List<TraceCertificationVo> parse(String certificationJson) {
        if (StringUtils.isBlank(certificationJson)) {
            return Collections.emptyList();
        }
        try {
            List<TraceCertificationVo> list = JSON.parseObject(certificationJson,
                new TypeReference<List<TraceCertificationVo>>() {
                });
            return list == null ? Collections.emptyList() : list;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
