package com.ym.farm.task.miniapp.batch;

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
 * 农事任务小程序种植批次查询。
 */
@RestController
@RequestMapping("/miniapp/smart-farming/stask/planting-batches")
public class FarmTaskPlantingBatchController {

    @DubboReference
    private RemoteAgricultureService agricultureService;

    @GetMapping("/active")
    public R<List<RemotePlantingBatchSummaryVo>> active(
        @RequestParam(required = false) List<Long> fieldIds) {
        return R.ok(agricultureService.listActivePlantingBatches(fieldIds));
    }
}
