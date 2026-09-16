package com.ym.agriculture.farming.satellite.health.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farming.satellite.health.model.vo.SatelliteHealthVo;
import com.ym.agriculture.farming.satellite.health.service.ISatelliteHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 卫星综合健康评分查询 API。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/smart-farming/satellite/health")
@SaCheckPermission("smartfarming:satelliteHealth:query")
public class SatelliteHealthController {

    private final ISatelliteHealthService healthService;

    /** 分页查询当前租户的启用地块。 */
    @GetMapping("/fields")
    public R<PageResult<SatelliteHealthVo.Field>> fields(@RequestParam(required = false) String keyword, PageQuery pageQuery) {
        return R.ok(healthService.queryFields(keyword, pageQuery));
    }

    /** 查询指定地块在统一排序下所在的分页。 */
    @GetMapping("/fields/{fieldId}/position")
    public R<Long> position(@PathVariable Long fieldId, @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(healthService.getFieldPage(fieldId, pageSize));
    }

    /** 查询当前活跃批次的有效日期卡片，首次最多返回最近 24 期。 */
    @GetMapping("/fields/{fieldId}/dates")
    public R<SatelliteHealthVo.Dates> dates(@PathVariable Long fieldId,
                                             @RequestParam(required = false) String beforeDate,
                                             @RequestParam(defaultValue = "24") int limit) {
        return R.ok(healthService.queryDates(fieldId, beforeDate, limit));
    }

    /** 查询指定影像日期的综合评分及指标明细。 */
    @GetMapping("/fields/{fieldId}/dates/{imageDate}")
    public R<SatelliteHealthVo.Detail> detail(@PathVariable Long fieldId, @PathVariable String imageDate) {
        return R.ok(healthService.queryDetail(fieldId, imageDate));
    }
}
