package com.ym.agriculture.farming.satellite.health.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.satellite.health.model.vo.SatelliteHealthVo;

/** 卫星综合健康评分只读服务。 */
public interface ISatelliteHealthService {

    /** 查询当前租户可查看的启用地块。 */
    PageResult<SatelliteHealthVo.Field> queryFields(String keyword, PageQuery pageQuery);

    /** 查询目标地块所在页码。 */
    long getFieldPage(Long fieldId, int pageSize);

    /** 查询地块当前活跃批次的历史有效影像日期。 */
    SatelliteHealthVo.Dates queryDates(Long fieldId, String beforeDate, int limit);

    /** 查询指定影像日期的评分详情。 */
    SatelliteHealthVo.Detail queryDetail(Long fieldId, String imageDate);
}
