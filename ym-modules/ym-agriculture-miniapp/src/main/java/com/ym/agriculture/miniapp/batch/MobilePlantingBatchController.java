package com.ym.agriculture.miniapp.batch;

import com.ym.agriculture.api.farming.RemoteAgricultureService;
import com.ym.agriculture.api.farming.domain.vo.RemotePlantingBatchSummaryVo;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 农业小程序种植批次查询。
 */
@RestController
@RequestMapping("/mobile/smart-farming/planting-batches")
public class MobilePlantingBatchController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/active")
    public R<List<RemotePlantingBatchSummaryVo>> active(
        @RequestParam(required = false) List<Long> fieldIds) {
        return R.ok(agricultureService.listActivePlantingBatches(fieldIds));
    }
}
