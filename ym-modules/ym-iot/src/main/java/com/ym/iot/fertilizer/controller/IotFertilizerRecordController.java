package com.ym.iot.fertilizer.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.iot.fertilizer.domain.bo.IotFertilizerRecordBo;
import com.ym.iot.fertilizer.domain.vo.IotFertilizerRecordVo;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 施肥机-单次施肥流水查询接口（仅查询，记录由 JetLinks 业务事件投影写入）。
 *
 * <p>路径：{@code /iot/fertilizer/record}，表 {@code iot_fertilizer_record}
 * 历史查询按记录所属租户过滤，登录用户仅能访问本租户的施肥流水。
 *
 * <p>对应告警查询请使用现有 {@code /iot/alert/record/page?alertType=FERTILIZER_BIT}。
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/fertilizer/record")
public class IotFertilizerRecordController extends BaseController {

    private final com.ym.iot.jetlinks.service.IJetLinksHistoryService jetLinksHistory;

    /**
     * 分页查询施肥流水。
     *
     * @param bo 查询条件（设备/类型/时间范围）
     * @param pageQuery 分页
     */
    @SaCheckPermission(
            value = {"iot:fertilizer:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<IotFertilizerRecordVo>> page(
            IotFertilizerRecordBo bo, PageQuery pageQuery) {
        return R.ok(jetLinksHistory.fertilizer(bo, pageQuery));
    }

    /** 根据主键查询施肥流水详情。 */
    @SaCheckPermission(
            value = {"iot:fertilizer:list", "iot:device:list", "iot:device:query"},
            mode = SaMode.OR)
    @GetMapping("/{recordId}")
    public R<IotFertilizerRecordVo> getInfo(@PathVariable Long recordId) {
        return R.ok(jetLinksHistory.fertilizer(recordId));
    }
}
