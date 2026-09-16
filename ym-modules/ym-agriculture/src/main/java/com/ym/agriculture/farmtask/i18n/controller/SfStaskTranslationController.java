package com.ym.agriculture.farmtask.i18n.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ym.common.core.domain.R;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.common.web.core.BaseController;
import com.ym.agriculture.shared.i18n.model.vo.SfI18nTextVo;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.farmtask.i18n.StaskI18nBackfillService;
import com.ym.agriculture.farmtask.i18n.StaskI18nPretranslateService;
import com.ym.agriculture.farmtask.i18n.model.bo.SfStaskTranslationBackfillBo;
import com.ym.agriculture.farmtask.i18n.model.bo.SfStaskTranslationCorrectionBo;
import com.ym.agriculture.farmtask.i18n.model.vo.SfStaskFarmWorkDictPretranslateVo;
import com.ym.agriculture.farmtask.i18n.model.vo.SfStaskPretranslateVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * stask 中维业务文本翻译运维接口。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/smart-farming/stask/translations")
public class SfStaskTranslationController extends BaseController {

    private final com.ym.agriculture.shared.i18n.StaskMessageResolver messages;
    private final ISfI18nTextService i18nTextService;
    private final StaskI18nBackfillService backfillService;
    private final StaskI18nPretranslateService pretranslateService;

    @SaCheckPermission("smartfarming:staskTranslation:list")
    @GetMapping("/page")
    public R<PageResult<SfI18nTextVo>> page(@RequestParam(required = false) String status,
        @RequestParam(required = false) String sourceKeyword,
        PageQuery pageQuery) {
        return R.ok(i18nTextService.page(requireTenantId(), status, sourceKeyword, pageQuery));
    }

    @SaCheckPermission("smartfarming:staskTranslation:edit")
    @PutMapping("/{translationId}")
    public R<Void> correct(@NotNull @PathVariable Long translationId,
        @Valid @RequestBody SfStaskTranslationCorrectionBo bo) {
        boolean corrected = i18nTextService.correct(requireTenantId(), translationId, bo.getTranslatedText(),
            bo.getSourceHash());
        if (!corrected) {
            throw messages.exception(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.TRANSLATION_SOURCE_HASH_INVALID);
        }
        return toAjax(corrected);
    }

    @SaCheckPermission("smartfarming:staskTranslation:retry")
    @PostMapping("/{translationId}/retry")
    public R<Void> retry(@NotNull @PathVariable Long translationId) {
        return toAjax(i18nTextService.retry(requireTenantId(), translationId));
    }

    @SaCheckPermission("smartfarming:staskTranslation:backfill")
    @PostMapping("/backfill")
    public R<Integer> backfill(@Valid @RequestBody(required = false) SfStaskTranslationBackfillBo bo) {
        String currentTenantId = requireTenantId();
        String tenantId = bo == null || StringUtils.isBlank(bo.getTenantId())
            ? currentTenantId : bo.getTenantId();
        if (!StringUtils.equals(currentTenantId, tenantId) && !LoginHelper.isSuperAdmin()) {
            throw messages.exception(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.TRANSLATION_TENANT_SCOPE_DENIED);
        }
        List<String> resourceTypes = bo == null ? List.of() : bo.getResourceTypes();
        return R.ok(TenantHelper.dynamic(tenantId, () -> backfillService.backfill(tenantId, resourceTypes)));
    }

    /**
     * 为全部有效租户预登记农事字典维文翻译待办。
     *
     * @return 扫描到的租户、字典和非空候选资源统计
     */
    @SaCheckPermission("smartfarming:staskTranslation:backfill")
    @PostMapping("/farm-work-dict/pretranslate")
    public R<SfStaskFarmWorkDictPretranslateVo> pretranslateFarmWorkDict() {
        if (!LoginHelper.isSuperAdmin()) {
            throw messages.exception(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.TRANSLATION_PRETRANSLATE_SUPER_ADMIN_REQUIRED);
        }
        return R.ok(backfillService.pretranslateFarmWorkDict());
    }

    /**
     * 为全部有效租户预登记 stask、农业引用数据及当前天气窗口的维文词条。
     *
     * <p>本接口只扫描业务数据并登记翻译待办，不在请求线程调用翻译供应商。</p>
     *
     * @return 全租户预处理统计
     */
    @SaCheckPermission("smartfarming:staskTranslation:backfill")
    @PostMapping("/pretranslate")
    public R<SfStaskPretranslateVo> pretranslate() {
        if (!LoginHelper.isSuperAdmin()) {
            throw messages.exception(
                com.ym.agriculture.shared.i18n.StaskMessageKeys.TRANSLATION_PRETRANSLATE_SUPER_ADMIN_REQUIRED);
        }
        return R.ok(pretranslateService.pretranslate());
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw messages.exception(com.ym.agriculture.shared.i18n.StaskMessageKeys.ERROR_COMMON_TENANT_CONTEXT_MISSING);
        }
        return tenantId;
    }
}
