package com.ym.iot.product.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.iot.product.domain.bo.IotProductPropertyBo;
import com.ym.iot.product.domain.vo.IotProductPropertyVo;
import com.ym.iot.product.service.IIotProductPropertyService;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物模型属性
 *
 * <p>管理物模型产品的属性（如温度、湿度等），一个产品下有多个属性。 基础路径：{@code /iot/property}，表 {@code iot_product_property}
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/property")
public class IotPropertyController extends BaseController {

    private final IIotProductPropertyService propertyService;

    /**
     * 查询物模型属性列表（不分页）
     *
     * @param bo 查询条件（productId、propertyKey 等），见 {@link IotProductPropertyBo}
     * @return 统一响应，{@code data} 为属性列表
     */
    @SaCheckPermission(
            value = {
                "iot:property:list",
                "iot:property:query",
                "iot:product:list",
                "iot:product:query",
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmConfig:query",
                "iot:alarmConfig:list"
            },
            mode = SaMode.OR)
    @GetMapping("/list")
    public R<List<IotProductPropertyVo>> list(IotProductPropertyBo bo) {
        return R.ok(propertyService.queryList(bo));
    }

    /**
     * 分页查询物模型属性列表
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数
     * @return 分页结果，{@code rows} 为当前页，{@code total} 为总条数
     */
    @SaCheckPermission(
            value = {
                "iot:property:list",
                "iot:property:query",
                "iot:product:list",
                "iot:product:query",
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmConfig:query",
                "iot:alarmConfig:list"
            },
            mode = SaMode.OR)
    @GetMapping("/page")
    public R<PageResult<IotProductPropertyVo>> page(IotProductPropertyBo bo, PageQuery pageQuery) {
        return R.ok(propertyService.queryPageList(bo, pageQuery));
    }

    /**
     * 根据主键查询物模型属性详情
     *
     * @param propertyId 属性主键
     * @return 统一响应，{@code data} 为属性详情
     */
    @SaCheckPermission(
            value = {
                "iot:property:list",
                "iot:property:query",
                "iot:product:list",
                "iot:product:query",
                "iot:device:list",
                "iot:device:query",
                "iot:monitor:list",
                "iot:video:list",
                "iot:fertilizer:list",
                "iot:motorvalve:list",
                "sf:field:list",
                "iot:alarmConfig:add",
                "iot:alarmConfig:edit",
                "iot:alarmConfig:query",
                "iot:alarmConfig:list"
            },
            mode = SaMode.OR)
    @GetMapping("/{propertyId}")
    public R<IotProductPropertyVo> getInfo(@PathVariable Long propertyId) {
        return R.ok(propertyService.queryById(propertyId));
    }
}
