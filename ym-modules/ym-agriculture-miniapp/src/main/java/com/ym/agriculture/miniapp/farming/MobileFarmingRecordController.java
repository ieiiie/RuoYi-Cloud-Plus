package com.ym.agriculture.miniapp.farming;

import com.ym.agriculture.api.farming.RemoteAgricultureMobileService;
import com.ym.agriculture.api.farming.domain.vo.RemoteAgricultureViewVo;
import com.ym.agriculture.miniapp.support.MobileRequestAdapter;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.domain.R;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 移动端农事记录完整读写兼容接口。 */
@RestController
@RequestMapping("/mobile/smart-farming/farming/farm-records")
public class MobileFarmingRecordController {

    @DubboReference
    private RemoteAgricultureMobileService agricultureService;

    @GetMapping("/page")
    public R<PageResult<RemoteAgricultureViewVo>> page(@RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.pageFarmingRecords(MobileRequestAdapter.query(parameters)));
    }

    @GetMapping("/latest-images")
    public R<RemoteAgricultureViewVo> latestImages(@RequestParam(required = false) Long fieldId,
                                                    @RequestParam(required = false) Integer limit) {
        return R.ok(agricultureService.latestFarmingImages(fieldId, limit));
    }

    @GetMapping("/{recordId}")
    public R<RemoteAgricultureViewVo> detail(@PathVariable Long recordId) {
        return R.ok(agricultureService.getFarmingRecord(recordId));
    }

    @GetMapping("/sensor-preview")
    public R<RemoteAgricultureViewVo> sensorPreview(@RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.farmingSensorPreview(MobileRequestAdapter.query(parameters), false));
    }

    @GetMapping("/sensor-simple-preview")
    public R<RemoteAgricultureViewVo> sensorSimplePreview(
        @RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.farmingSensorPreview(MobileRequestAdapter.query(parameters), true));
    }

    @GetMapping("/weather-preview")
    public R<RemoteAgricultureViewVo> weatherPreview(@RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.farmingWeatherPreview(MobileRequestAdapter.query(parameters)));
    }

    @GetMapping("/growth-stage-preview")
    public R<RemoteAgricultureViewVo> growthStagePreview(
        @RequestParam MultiValueMap<String, String> parameters) {
        return R.ok(agricultureService.farmingGrowthStagePreview(MobileRequestAdapter.query(parameters)));
    }

    @PostMapping("/drafts")
    public R<Object> addDraft(@RequestHeader(value = "X-Request-Id", required = false) String requestId,
                              @RequestBody Map<String, Object> body) {
        RemoteAgricultureViewVo result = agricultureService.addFarmingDraft(
            MobileRequestAdapter.command(requestId, "FARMING_DRAFT_CREATE", null, body));
        return R.ok(result.containsKey("recordId") ? result.get("recordId") : result);
    }

    @PutMapping("/drafts/{recordId}")
    public R<Void> updateDraft(@PathVariable Long recordId,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        agricultureService.updateFarmingDraft(recordId,
            MobileRequestAdapter.command(requestId, "FARMING_DRAFT_UPDATE", recordId, body));
        return R.ok();
    }

    @PostMapping("/drafts/{recordId}/submit")
    public R<Void> submit(@PathVariable Long recordId,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        agricultureService.submitFarmingDraft(recordId,
            MobileRequestAdapter.command(requestId, "FARMING_DRAFT_SUBMIT", recordId, Map.of()));
        return R.ok();
    }

    @PutMapping("/submitted/{recordId}")
    public R<Void> updateSubmitted(@PathVariable Long recordId,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody Map<String, Object> body) {
        agricultureService.updateSubmittedFarmingRecord(recordId,
            MobileRequestAdapter.command(requestId, "FARMING_SUBMITTED_UPDATE", recordId, body));
        return R.ok();
    }

    @DeleteMapping("/drafts/{recordId}")
    public R<Void> removeDraft(@PathVariable Long recordId,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        agricultureService.deleteFarmingRecord(recordId, false,
            MobileRequestAdapter.command(requestId, "FARMING_DRAFT_DELETE", recordId, Map.of()));
        return R.ok();
    }

    @DeleteMapping("/submitted/{recordId}")
    public R<Void> removeSubmitted(@PathVariable Long recordId,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        agricultureService.deleteFarmingRecord(recordId, true,
            MobileRequestAdapter.command(requestId, "FARMING_SUBMITTED_DELETE", recordId, Map.of()));
        return R.ok();
    }

    @PatchMapping("/{recordId}/media/{mediaId}")
    public R<RemoteAgricultureViewVo> patchMediaGeo(@PathVariable Long recordId, @PathVariable Long mediaId,
        @RequestHeader(value = "X-Request-Id", required = false) String requestId,
        @RequestBody(required = false) Map<String, Object> body) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>(body == null ? Map.of() : body);
        payload.put("mediaId", mediaId);
        return R.ok(agricultureService.patchFarmingMedia(recordId, mediaId,
            MobileRequestAdapter.command(requestId, "FARMING_MEDIA_PATCH", recordId, payload)));
    }
}
