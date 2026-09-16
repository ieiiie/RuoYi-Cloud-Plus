package com.ym.agriculture.miniapp.news;

import com.ym.agriculture.api.farming.RemoteAgricultureMobileService;
import com.ym.agriculture.api.farming.domain.vo.RemoteAgricultureViewVo;
import com.ym.agriculture.miniapp.support.MobileRequestAdapter;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 移动端农业资讯兼容接口。 */
@RestController
@RequestMapping("/mobile/smart-farming/news")
public class MobileNewsController {

    @DubboReference
    private RemoteAgricultureMobileService agricultureService;

    @GetMapping("/page")
    public R<PageResult<RemoteAgricultureViewVo>> page(@RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.pageNews(MobileRequestAdapter.query(parameters)));
    }

    @GetMapping("/{articleId}")
    public R<RemoteAgricultureViewVo> detail(@PathVariable Long articleId) {
        return R.ok(agricultureService.getNews(articleId));
    }

    @GetMapping("/dashboard")
    public R<RemoteAgricultureViewVo> dashboard() {
        return R.ok(agricultureService.newsDashboard());
    }
}
