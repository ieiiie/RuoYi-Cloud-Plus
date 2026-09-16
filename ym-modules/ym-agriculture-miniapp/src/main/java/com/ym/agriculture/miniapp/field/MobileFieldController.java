package com.ym.agriculture.miniapp.field;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemoteFieldSummaryVo;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农业小程序地块查询聚合接口。
 *
 * @author ym-cloud
 */
@RestController
@RequestMapping("/mobile/smart-farming/fields")
public class MobileFieldController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/list")
    public R<List<RemoteFieldSummaryVo>> list() {
        return R.ok(agricultureService.listFields());
    }
}
