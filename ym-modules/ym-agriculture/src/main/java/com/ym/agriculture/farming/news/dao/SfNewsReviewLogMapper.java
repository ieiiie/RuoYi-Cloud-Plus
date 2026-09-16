package com.ym.agriculture.farming.news.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.agriculture.farming.news.model.entity.SfNewsReviewLog;

/** 平台全局农业资讯审核日志 Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfNewsReviewLogMapper extends BaseMapperPlus<SfNewsReviewLog, SfNewsReviewLog> {
}
