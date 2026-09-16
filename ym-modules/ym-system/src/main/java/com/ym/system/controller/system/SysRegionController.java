package com.ym.system.controller.system;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.domain.R;
import com.ym.common.web.core.BaseController;
import com.ym.system.domain.vo.SysRegionTreeVo;
import com.ym.system.domain.vo.SysRegionVo;
import com.ym.system.service.ISysRegionService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 系统行政区划只读查询接口。 */
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/region")
public class SysRegionController extends BaseController {

    private final ISysRegionService regionService;

    /** 查询行政区划；不传 parentId 时返回全部，传值时返回直接下级。 */
    @GetMapping("/children")
    public R<List<SysRegionVo>> children(@RequestParam(required = false) Long parentId) {
        return R.ok(regionService.listChildren(parentId));
    }

    /** 按名称、完整名称或 adcode 前缀搜索，不限制返回条数。 */
    @GetMapping("/search")
    public R<List<SysRegionVo>> search(
        @RequestParam @NotBlank(message = "搜索关键词不能为空") String keyword,
        @RequestParam(required = false)
        @Pattern(regexp = "^(province|city|district|street)$", message = "行政区划层级不正确") String regionLevel) {
        return R.ok(regionService.search(keyword, regionLevel));
    }

    /** 以指定节点为根返回整棵行政区划子树。 */
    @GetMapping("/tree")
    public R<SysRegionTreeVo> tree(@RequestParam Long regionId) {
        return R.ok(regionService.treeByRootId(regionId));
    }

    /** 按 adcode 查询详情。 */
    @GetMapping("/{adcode}")
    public R<SysRegionVo> getInfo(@PathVariable String adcode) {
        return R.ok(regionService.selectByAdcode(adcode));
    }
}
