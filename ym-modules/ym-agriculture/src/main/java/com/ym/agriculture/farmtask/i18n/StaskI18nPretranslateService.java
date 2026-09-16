package com.ym.agriculture.farmtask.i18n;

import com.ym.agriculture.shared.i18n.StaskI18nScanResult;

import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.batch.dao.SfPlantingBatchMapper;
import com.ym.agriculture.farming.batch.model.entity.SfPlantingBatch;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.dao.SfCropVarietyMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farming.crop.model.entity.SfCropVariety;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.entity.SfField;
import com.ym.agriculture.farming.field.support.SfMasterTenantDistrictAccessor;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.shared.i18n.model.constants.I18nLocale;
import com.ym.agriculture.shared.i18n.model.vo.SfI18nTextVo;
import com.ym.agriculture.shared.i18n.service.ISfI18nTextService;
import com.ym.agriculture.farming.i18n.support.SmartFarmingI18nSourceFactory;
import com.ym.agriculture.farmtask.i18n.model.vo.SfStaskPretranslateVo;
import com.ym.agriculture.farming.weather.dao.SfWeatherForecastMapper;
import com.ym.agriculture.farming.weather.dao.SfWeatherLiveMapper;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherForecast;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherLive;
import com.ym.agriculture.farming.weather.support.SfWeatherI18nSourceFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 全租户 stask、农业引用数据及当前天气窗口业务文本预处理服务。
 *
 * <p>业务数据按主键游标分批读取，每批在独立事务中登记词条。外部翻译由提交后的 Worker
 * 异步执行，本服务不会调用翻译供应商。</p>
 */
@Service
@RequiredArgsConstructor
public class StaskI18nPretranslateService {

    static final int BATCH_SIZE = 200;

    private final ISfI18nTextService i18nTextService;
    private final StaskI18nBackfillService staskBackfillService;
    private final StaskTenantI18nPretranslateReader tenantReader;
    private final StaskEmployeeI18nPretranslateReader employeeReader;
    private final SfFieldMapper fieldMapper;
    private final SfCropSpeciesMapper speciesMapper;
    private final SfCropVarietyMapper varietyMapper;
    private final SfPlantingBatchMapper plantingBatchMapper;
    private final SfWeatherForecastMapper weatherForecastMapper;
    private final SfWeatherLiveMapper weatherLiveMapper;
    private final SfMasterTenantDistrictAccessor tenantDistrictAccessor;
    private final PlatformTransactionManager transactionManager;

    /**
     * 扫描全部有效租户的 stask、农业引用数据及当前天气窗口并幂等登记翻译词条。
     *
     * @return 全租户扫描和词条登记统计
     */
    public SfStaskPretranslateVo pretranslate() {
        List<String> tenantIds = selectTenantIds();
        long resourceCount = 0;
        long sourceTextCount = 0;
        long uniquePhraseCount = 0;
        long existingPhraseCount = 0;
        for (String tenantId : tenantIds) {
            TenantPretranslateSummary summary = TenantHelper.dynamic(tenantId,
                () -> pretranslateTenant(tenantId));
            resourceCount += summary.scanResult().resourceCount();
            sourceTextCount += summary.scanResult().sourceTextCount();
            uniquePhraseCount += summary.scanResult().sourceTexts().size();
            existingPhraseCount += summary.existingPhraseCount();
        }
        return new SfStaskPretranslateVo(tenantIds.size(), resourceCount, sourceTextCount,
            uniquePhraseCount, uniquePhraseCount - existingPhraseCount, existingPhraseCount);
    }

    private TenantPretranslateSummary pretranslateTenant(String tenantId) {
        Set<PhraseIdentity> existing = activePhraseIdentities(tenantId);
        StaskI18nScanResult result = staskBackfillService.backfillWithSummary(tenantId, List.of())
            .merge(scanFields(tenantId))
            .merge(scanSpecies(tenantId))
            .merge(scanVarieties(tenantId))
            .merge(scanPlantingBatches(tenantId))
            .merge(scanEmployees(tenantId))
            .merge(scanWeather(tenantId));
        long existingCount = result.sourceTexts().stream()
            .map(PhraseIdentity::from)
            .filter(existing::contains)
            .count();
        return new TenantPretranslateSummary(result, existingCount);
    }

    private Set<PhraseIdentity> activePhraseIdentities(String tenantId) {
        List<SfI18nTextVo> rows = i18nTextService.list(tenantId, null, null);
        LinkedHashSet<PhraseIdentity> identities = new LinkedHashSet<>();
        for (SfI18nTextVo row : rows) {
            if (row != null && I18nLocale.UG_CN.equals(row.getLocale())
                && StringUtils.isNotBlank(row.getSourceText())) {
                String sourceHash = StringUtils.isBlank(row.getSourceHash())
                    ? DigestUtil.sha256Hex(row.getSourceText()) : row.getSourceHash().toLowerCase();
                identities.add(new PhraseIdentity(sourceHash, row.getSourceText()));
            }
        }
        return Set.copyOf(identities);
    }

    private List<String> selectTenantIds() {
        return tenantReader.listActiveTenantIds();
    }

    private StaskI18nScanResult scanFields(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfField> rows = fieldMapper.selectList(Wrappers.<SfField>lambdaQuery()
                .eq(SfField::getTenantId, tenantId)
                .eq(SfField::getDelFlag, SystemConstants.NORMAL)
                .eq(SfField::getFieldType, FieldType.GREENHOUSE)
                .gt(SfField::getFieldId, cursor)
                .orderByAsc(SfField::getFieldId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(registerBatch(tenantId, rows, SmartFarmingI18nSourceFactory::sources));
            cursor = rows.get(rows.size() - 1).getFieldId();
        }
    }

    private StaskI18nScanResult scanSpecies(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfCropSpecies> rows = speciesMapper.selectList(Wrappers.<SfCropSpecies>lambdaQuery()
                .eq(SfCropSpecies::getTenantId, tenantId)
                .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL)
                .gt(SfCropSpecies::getSpeciesId, cursor)
                .orderByAsc(SfCropSpecies::getSpeciesId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(registerBatch(tenantId, rows, SmartFarmingI18nSourceFactory::sources));
            cursor = rows.get(rows.size() - 1).getSpeciesId();
        }
    }

    private StaskI18nScanResult scanVarieties(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfCropVariety> rows = varietyMapper.selectList(Wrappers.<SfCropVariety>lambdaQuery()
                .eq(SfCropVariety::getTenantId, tenantId)
                .eq(SfCropVariety::getDelFlag, SystemConstants.NORMAL)
                .gt(SfCropVariety::getVarietyId, cursor)
                .orderByAsc(SfCropVariety::getVarietyId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(registerBatch(tenantId, rows, SmartFarmingI18nSourceFactory::sources));
            cursor = rows.get(rows.size() - 1).getVarietyId();
        }
    }

    private StaskI18nScanResult scanPlantingBatches(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            List<SfPlantingBatch> rows = plantingBatchMapper.selectList(Wrappers.<SfPlantingBatch>lambdaQuery()
                .eq(SfPlantingBatch::getTenantId, tenantId)
                .eq(SfPlantingBatch::getDelFlag, SystemConstants.NORMAL)
                .gt(SfPlantingBatch::getBatchId, cursor)
                .orderByAsc(SfPlantingBatch::getBatchId)
                .last("limit " + BATCH_SIZE));
            if (rows.isEmpty()) {
                return summary;
            }
            summary = summary.merge(registerBatch(tenantId, rows, SmartFarmingI18nSourceFactory::sources));
            cursor = rows.get(rows.size() - 1).getBatchId();
        }
    }

    private StaskI18nScanResult scanEmployees(String tenantId) {
        StaskI18nScanResult summary = StaskI18nScanResult.empty();
        long cursor = 0L;
        while (true) {
            StaskEmployeeI18nPretranslateReader.EmployeeI18nBatch batch =
                employeeReader.readBatch(tenantId, cursor, BATCH_SIZE);
            if (batch.resourceCount() == 0) {
                return summary;
            }
            inBatchTransaction(() -> i18nTextService.registerTexts(tenantId, batch.sources()));
            summary = summary.merge(StaskI18nScanResult.of(batch.resourceCount(), batch.sources()));
            cursor = batch.nextCursor();
        }
    }

    private StaskI18nScanResult scanWeather(String tenantId) {
        String adcode = tenantDistrictAccessor.selectNormalizedDistrictCode(tenantId);
        if (StringUtils.isBlank(adcode)) {
            return StaskI18nScanResult.empty();
        }
        List<I18nTextSource> sources = new ArrayList<>();
        int resourceCount = 0;
        SfWeatherLive live = weatherLiveMapper.selectLatestOneByAdcode(adcode);
        if (live != null) {
            resourceCount++;
            sources.addAll(SfWeatherI18nSourceFactory.sources(live));
        }
        SfWeatherForecast latest = weatherForecastMapper.selectLatestOneByAdcode(adcode);
        if (latest != null && latest.getSyncId() != null) {
            List<SfWeatherForecast> forecasts = weatherForecastMapper
                .selectListBySyncIdOrderByCastDateAsc(latest.getSyncId()).stream()
                .filter(row -> row != null && row.getCastDate() != null)
                .filter(row -> row.getCastDate().compareTo(DateUtil.today()) >= 0)
                .toList();
            resourceCount += forecasts.size();
            forecasts.forEach(row -> sources.addAll(SfWeatherI18nSourceFactory.sources(row)));
        }
        if (!sources.isEmpty()) {
            inBatchTransaction(() -> i18nTextService.registerTexts(tenantId, sources));
        }
        return StaskI18nScanResult.of(resourceCount, sources);
    }

    private <T> StaskI18nScanResult registerBatch(String tenantId, Collection<T> rows,
        java.util.function.Function<T, List<I18nTextSource>> sourceFactory) {
        List<I18nTextSource> sources = new ArrayList<>();
        rows.stream().filter(Objects::nonNull).forEach(row -> sources.addAll(sourceFactory.apply(row)));
        inBatchTransaction(() -> i18nTextService.registerTexts(tenantId, sources));
        return StaskI18nScanResult.of(rows.size(), sources);
    }

    private void inBatchTransaction(Runnable action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(status -> action.run());
    }

    private record PhraseIdentity(String sourceHash, String sourceText) {
        private static PhraseIdentity from(String sourceText) {
            return new PhraseIdentity(DigestUtil.sha256Hex(sourceText), sourceText);
        }
    }

    private record TenantPretranslateSummary(StaskI18nScanResult scanResult, long existingPhraseCount) {
    }
}
