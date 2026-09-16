package com.ym.iot.product.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import com.ym.common.excel.utils.ExcelBuilder;
import com.ym.common.log.annotation.Log;
import com.ym.common.log.enums.BusinessType;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.web.core.BaseController;
import com.ym.iot.product.domain.bo.IotProductBo;
import com.ym.iot.product.domain.vo.IotProductExportVo;
import com.ym.iot.product.domain.vo.IotProductVo;
import com.ym.iot.product.service.IIotProductService;

import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 物模型产品
 *
 * <p>提供物模型产品查询和导出；档案及物模型由 JetLinks 统一维护。 基础路径：{@code /iot/product} 数据源：{@code iot} 库，表 {@code
 * iot_product}
 *
 * @author ym-cloud
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/iot/product")
public class IotProductController extends BaseController {

    private final IIotProductService productService;

    /**
     * 查询物模型产品列表（不分页）
     *
     * @param bo 查询条件（productKey、productName、status、deviceCategory 等），见 {@link IotProductBo}
     * @return 统一响应，{@code data} 为产品列表
     */
    @SaCheckPermission(
            value = {
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
    public R<List<IotProductVo>> list(IotProductBo bo) {
        return R.ok(productService.queryList(bo));
    }

    /**
     * 分页查询物模型产品列表
     *
     * @param bo 查询条件
     * @param pageQuery 分页参数（pageNum、pageSize）
     * @return 分页结果，{@code rows} 为当前页，{@code total} 为总条数
     */
    @SaCheckPermission(
            value = {
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
    public R<PageResult<IotProductVo>> page(IotProductBo bo, PageQuery pageQuery) {
        return R.ok(productService.queryPageList(bo, pageQuery));
    }

    /**
     * 根据主键查询物模型产品详情（含 {@code mapIconUrl}、{@code mapSelectedIconUrl} 地图图标）
     *
     * @param productId 产品主键
     * @return 统一响应，{@code data} 为产品详情
     */
    @SaCheckPermission(
            value = {
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
    @GetMapping("/{productId}")
    public R<IotProductVo> getInfo(@PathVariable Long productId) {
        return R.ok(productService.queryById(productId));
    }

    /**
     * 导出物模型产品 Excel（筛选条件与列表一致）
     *
     * @param bo 查询条件
     * @param response HTTP 响应，流式写出文件
     */
    @Log(title = "物模型产品", businessType = BusinessType.EXPORT)
    @SaCheckPermission("iot:product:export")
    @PostMapping("/export")
    public void export(IotProductBo bo, HttpServletResponse response) {
        List<IotProductExportVo> list = productService.queryExportList(bo);
        ExcelBuilder.of(list, IotProductExportVo.class).sheetName("物模型产品").toResponse(response);
    }
}
