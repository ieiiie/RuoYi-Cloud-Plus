package com.ym.iot.jetlinks.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;

import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksCategoryService;
import com.ym.jetlinks.rpc.CategoryDto;

import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** The provider catalog is the only product-category source; no dictionary mapping. */
@Validated
@RestController
@ConditionalOnJetLinks
@RequiredArgsConstructor
@RequestMapping("/iot/product/category")
public class JetLinksCategoryController extends BaseController {
    private final IJetLinksCategoryService categoryService;

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
    public R<List<CategoryDto>> list() {
        return R.ok(categoryService.listCategories());
    }
}
