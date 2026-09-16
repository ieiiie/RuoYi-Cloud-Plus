package com.ym.agriculture.miniapp.farmwork;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkTreeVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteFarmWorkVo;
import com.ym.common.core.domain.R;
import jakarta.validation.constraints.NotNull;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农业小程序农事选项聚合接口。
 */
@Validated
@RestController
@RequestMapping("/mobile/smart-farming/farming/farm-work")
public class MobileFarmWorkController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/tree")
    public R<List<RemoteFarmWorkTreeVo>> tree() {
        return R.ok(agricultureService.listEnabledFarmWorkTree());
    }

    @GetMapping("/categories")
    public R<List<RemoteFarmWorkVo>> categories() {
        return R.ok(agricultureService.listFarmWorkCategories());
    }

    @GetMapping("/items")
    public R<List<RemoteFarmWorkVo>> items(
        @NotNull(message = "农事分类不能为空") @RequestParam Long categoryId) {
        return R.ok(agricultureService.listEnabledFarmWorkItems(categoryId));
    }
}
