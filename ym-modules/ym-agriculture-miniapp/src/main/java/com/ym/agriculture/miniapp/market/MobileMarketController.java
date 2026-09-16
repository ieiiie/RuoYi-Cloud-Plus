package com.ym.agriculture.miniapp.market;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.bo.RemoteMarketQuoteQueryBo;
import com.ym.agriculture.api.farming.domain.vo.RemoteMarketCategoryVo;
import com.ym.agriculture.api.farming.domain.vo.RemoteMarketQuoteVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 农业小程序农业行情查询接口。 */
@Validated
@RestController
@RequestMapping("/mobile/smart-farming/market")
public class MobileMarketController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/quotes/page")
    public R<PageResult<RemoteMarketQuoteVo>> page(RemoteMarketQuoteQueryBo query) {
        return R.ok(agricultureService.pageMarketQuotes(query));
    }

    @GetMapping("/categories")
    public R<List<RemoteMarketCategoryVo>> categories() {
        return R.ok(agricultureService.listMarketCategories());
    }
}
