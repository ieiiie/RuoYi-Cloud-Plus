package com.ym.agriculture.farming.trace.service;

import com.ym.agriculture.farming.trace.model.vo.SfTracePublicVo;
import jakarta.servlet.http.HttpServletRequest;

/**
 * H5 公开溯源查询服务。
 */
public interface ISfTracePublicService {

    SfTracePublicVo queryPublic(String traceCode, HttpServletRequest request);
}
