package com.ym.agriculture.farming.weatheralert.service.impl;

import cn.hutool.core.thread.ThreadUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.field.support.SfMasterTenantRegionAccessor;
import com.ym.agriculture.farming.weatheralert.config.NmcWeatherAlertProperties;
import com.ym.agriculture.farming.weatheralert.dao.SfWeatherAlertMapper;
import com.ym.agriculture.farming.weatheralert.dao.SfWeatherAlertSyncMetaMapper;
import com.ym.agriculture.farming.weatheralert.model.entity.SfWeatherAlert;
import com.ym.agriculture.farming.weatheralert.model.entity.SfWeatherAlertSyncMeta;
import com.ym.agriculture.farming.weatheralert.model.vo.SfWeatherAlertSummaryVo;
import com.ym.agriculture.farming.weatheralert.model.vo.SfWeatherAlertVo;
import com.ym.agriculture.farming.weatheralert.remote.NmcWeatherAlertApi;
import com.ym.agriculture.farming.weatheralert.remote.dto.NmcFindAlarmResponse;
import com.ym.agriculture.farming.weatheralert.service.ISfWeatherAlertService;
import com.ym.agriculture.farming.weatheralert.support.NmcAlertDetailHtmlParser;
import com.ym.agriculture.farming.weatheralert.support.NmcAlertParser;
import com.ym.agriculture.farming.weatheralert.support.NmcProvinceNameResolver;
import com.ym.agriculture.farming.weatheralert.support.WeatherAlertRegionMatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import retrofit2.Response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 气象预警：NMC 公开列表同步 + 本地缓存查询。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfWeatherAlertServiceImpl implements ISfWeatherAlertService {

    private static final ZoneId BEIJING = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter OUT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter[] IN_FMTS = {
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    };

    public static final String FRESH = "FRESH";
    public static final String STALE = "STALE";
    public static final String UNAVAILABLE = "UNAVAILABLE";

    private final NmcWeatherAlertProperties properties;
    private final ObjectProvider<NmcWeatherAlertApi> apiProvider;
    private final SfWeatherAlertMapper alertMapper;
    private final SfWeatherAlertSyncMetaMapper syncMetaMapper;
    private final SfMasterTenantRegionAccessor tenantRegionAccessor;
    private final NmcProvinceNameResolver provinceNameResolver;

    @Override
    public void syncEnabledProvinces() {
        if (!properties.isEnabled()) {
            log.debug("跳过气象预警同步：nmc-weather-alert.enabled=false");
            return;
        }
        NmcWeatherAlertApi api = apiProvider.getIfAvailable();
        if (api == null) {
            log.warn("跳过气象预警同步：NmcWeatherAlertApi 未装配");
            writeMeta(SfWeatherAlertSyncMeta.KEY_LAST_ATTEMPT_AT, Instant.now().toString());
            writeMeta(SfWeatherAlertSyncMeta.KEY_LAST_STATUS, "API_UNAVAILABLE");
            return;
        }
        Set<String> provinces = tenantRegionAccessor.listActiveProvinceCodes();
        if (provinces.isEmpty()) {
            log.info("气象预警同步：无启用租户省份，跳过");
            return;
        }
        writeMeta(SfWeatherAlertSyncMeta.KEY_LAST_ATTEMPT_AT, Instant.now().toString());
        int ok = 0;
        int fail = 0;
        int index = 0;
        for (String provinceCode : provinces) {
            if (index > 0) {
                ThreadUtil.sleep(Math.max(0L, properties.getProvinceIntervalMs()));
            }
            index++;
            try {
                syncProvince(api, provinceCode);
                ok++;
            } catch (Exception ex) {
                fail++;
                log.warn("气象预警省份同步失败 provinceCode={}", provinceCode, ex);
            }
        }
        if (ok > 0) {
            writeMeta(SfWeatherAlertSyncMeta.KEY_LAST_SUCCESS_AT, Instant.now().toString());
            writeMeta(SfWeatherAlertSyncMeta.KEY_LAST_STATUS, fail == 0 ? "SUCCESS" : "PARTIAL");
        } else {
            writeMeta(SfWeatherAlertSyncMeta.KEY_LAST_STATUS, "FAILED");
        }
        log.info("气象预警同步结束，省份成功={}，失败={}", ok, fail);
    }

    @Override
    public SfWeatherAlertSummaryVo summaryForCurrentTenant() {
        String freshness = resolveFreshnessStatus();
        List<SfWeatherAlert> matched = listMatchedForCurrentTenant(true);
        SfWeatherAlertSummaryVo vo = new SfWeatherAlertSummaryVo();
        vo.setFreshnessStatus(freshness);
        if (matched.isEmpty()) {
            vo.setHasAlert(false);
            vo.setAlertCount(0);
            if (UNAVAILABLE.equals(freshness)) {
                vo.setSummaryTitle("暂无预警数据");
            } else {
                vo.setSummaryTitle("暂无气象预警");
            }
            return vo;
        }
        matched.sort(Comparator
            .comparingInt((SfWeatherAlert a) -> NmcAlertParser.levelRank(a.getLevelCode())).reversed()
            .thenComparing(SfWeatherAlert::getEffectiveAt, Comparator.nullsLast(Comparator.reverseOrder())));
        SfWeatherAlert top = matched.get(0);
        vo.setHasAlert(true);
        vo.setAlertCount(matched.size());
        vo.setHighestLevelCode(top.getLevelCode());
        vo.setHighestLevelName(top.getLevelName());
        vo.setSummaryTitle(StringUtils.isNotBlank(top.getHeadline()) ? top.getHeadline() : top.getTitle());
        vo.setLatestEffectiveAt(formatOut(top.getEffectiveAt()));
        return vo;
    }

    @Override
    public PageResult<SfWeatherAlertVo> pageForCurrentTenant(Boolean activeOnly, PageQuery pageQuery) {
        boolean onlyActive = activeOnly == null || activeOnly;
        List<SfWeatherAlert> matched = listMatchedForCurrentTenant(onlyActive);
        matched.sort(Comparator.comparing(SfWeatherAlert::getEffectiveAt,
            Comparator.nullsLast(Comparator.reverseOrder())));
        String freshness = resolveFreshnessStatus();
        List<SfWeatherAlertVo> all = matched.stream()
            .map(row -> toVo(row, freshness, false))
            .collect(Collectors.toList());
        PageQuery pq = pageQuery != null ? pageQuery : new PageQuery(1, 10);
        int pageNum = pq.getPageNum() == null || pq.getPageNum() < 1 ? 1 : pq.getPageNum();
        int pageSize = pq.getPageSize() == null || pq.getPageSize() < 1 ? 10 : pq.getPageSize();
        if (pageSize == Integer.MAX_VALUE) {
            pageSize = 10;
        }
        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());
        Page<SfWeatherAlertVo> page = new Page<>(pageNum, pageSize, all.size());
        page.setRecords(all.subList(from, to));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public SfWeatherAlertVo detail(String warningId) {
        if (StringUtils.isBlank(warningId)) {
            throw new ServiceException("预警ID不能为空");
        }
        SfWeatherAlert row = alertMapper.selectByWarningId(warningId.trim());
        if (row == null) {
            throw new ServiceException("预警不存在或已失效");
        }
        WeatherAlertRegionMatcher.TenantRegion tenant = requireTenantRegion();
        if (!WeatherAlertRegionMatcher.matches(tenant, row.getProvinceCode(), row.getCityCode(), row.getDistrictCode())) {
            throw new ServiceException("无权查看该地区预警");
        }
        return toVo(row, resolveFreshnessStatus(), true);
    }

    private void syncProvince(NmcWeatherAlertApi api, String provinceCode) throws Exception {
        String provinceName = provinceNameResolver.resolveProvinceName(provinceCode);
        if (StringUtils.isBlank(provinceName)) {
            log.warn("无法解析省份名称，跳过 provinceCode={}", provinceCode);
            return;
        }
        Set<String> seen = new HashSet<>();
        Date now = new Date();
        int pageNo = 1;
        int totalPage = 1;
        while (pageNo <= totalPage && pageNo <= properties.getMaxPagesPerProvince()) {
            Response<NmcFindAlarmResponse> response = api.findAlarm(
                pageNo, properties.getPageSize(), "", "", provinceName).execute();
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("findAlarm HTTP " + response.code());
            }
            NmcFindAlarmResponse body = response.body();
            if (body.getCode() != null && body.getCode() != 0) {
                throw new IllegalStateException("findAlarm code=" + body.getCode() + " msg=" + body.getMsg());
            }
            NmcFindAlarmResponse.PageBody page = body.getData() != null ? body.getData().getPage() : null;
            if (page == null) {
                break;
            }
            if (page.getTotalPage() != null && page.getTotalPage() > 0) {
                totalPage = page.getTotalPage();
            }
            List<NmcFindAlarmResponse.AlarmItem> list = page.getList();
            if (list == null || list.isEmpty()) {
                break;
            }
            for (NmcFindAlarmResponse.AlarmItem item : list) {
                if (item == null || StringUtils.isBlank(item.getAlertid())) {
                    continue;
                }
                seen.add(item.getAlertid().trim());
                upsertFromListItem(api, item, provinceCode, now);
            }
            pageNo++;
        }
        markMissingAndDeactivate(provinceCode, seen, now);
    }

    private void upsertFromListItem(NmcWeatherAlertApi api, NmcFindAlarmResponse.AlarmItem item,
                                    String syncProvinceCode, Date now) {
        String warningId = item.getAlertid().trim();
        SfWeatherAlert existing = alertMapper.selectByWarningId(warningId);
        String areaAdcode = NmcAlertParser.extractAreaAdcode(warningId);
        NmcAlertParser.RegionCodes region = NmcAlertParser.toRegionCodes(areaAdcode);
        NmcAlertParser.LevelInfo level = NmcAlertParser.parseLevelFromTitle(item.getTitle());
        if (level.code() == null) {
            level = NmcAlertParser.parseLevelFromPic(item.getPic());
        }
        NmcAlertParser.TypeInfo type = NmcAlertParser.parseTypeFromTitle(item.getTitle());

        SfWeatherAlert row = existing != null ? existing : new SfWeatherAlert();
        row.setWarningId(warningId);
        row.setProvinceCode(region.provinceCode() != null ? region.provinceCode() : NmcAlertParser.pad6(syncProvinceCode));
        row.setCityCode(region.cityCode());
        row.setDistrictCode(region.districtCode());
        row.setTitle(item.getTitle());
        row.setHeadline(item.getTitle());
        row.setAlertTypeCode(type.code());
        row.setAlertTypeName(type.name());
        row.setLevelCode(level.code());
        row.setLevelName(level.name());
        if (StringUtils.isBlank(row.getPublisher())) {
            row.setPublisher(NmcAlertParser.guessPublisher(item.getTitle()));
        }
        row.setEffectiveAt(parseIssueTime(item.getIssuetime()));
        row.setAreaText(NmcAlertParser.guessAreaText(item.getTitle()));
        row.setSourceUrl(buildSourceUrl(item.getUrl()));
        row.setActiveFlag(1);
        row.setMissingCount(0);
        row.setLastSeenAt(now);
        row.setFreshnessStatus(FRESH);
        row.setUpdateTime(now);
        if (existing == null) {
            row.setCreateTime(now);
        }

        boolean needDetail = properties.isFetchDetail()
            && (existing == null
            || StringUtils.isBlank(existing.getDescription())
            || StringUtils.isBlank(existing.getInstructionJson()));
        if (needDetail && StringUtils.isNotBlank(row.getSourceUrl())) {
            fillDetailQuietly(api, row);
        }

        if (existing == null) {
            alertMapper.insert(row);
        } else {
            alertMapper.updateById(row);
        }
    }

    private void fillDetailQuietly(NmcWeatherAlertApi api, SfWeatherAlert row) {
        try {
            Response<ResponseBody> resp = api.fetchHtml(row.getSourceUrl()).execute();
            if (!resp.isSuccessful() || resp.body() == null) {
                return;
            }
            String html;
            try (ResponseBody body = resp.body()) {
                html = body.string();
            }
            NmcAlertDetailHtmlParser.ParsedDetail parsed = NmcAlertDetailHtmlParser.parse(html);
            if (StringUtils.isNotBlank(parsed.description())) {
                row.setDescription(parsed.description());
            }
            if (parsed.instructions() != null && !parsed.instructions().isEmpty()) {
                row.setInstructionJson(JSON.toJSONString(parsed.instructions()));
            }
            if (StringUtils.isNotBlank(parsed.publisher())) {
                row.setPublisher(parsed.publisher());
            }
        } catch (Exception ex) {
            log.debug("预警详情解析失败 warningId={} cause={}", row.getWarningId(), ex.toString());
        }
    }

    private void markMissingAndDeactivate(String provinceCode, Set<String> seen, Date now) {
        String padded = NmcAlertParser.pad6(provinceCode);
        List<SfWeatherAlert> actives = alertMapper.selectActiveByProvince(padded);
        int threshold = Math.max(1, properties.getDeactivateAfterMissing());
        for (SfWeatherAlert row : actives) {
            if (seen.contains(row.getWarningId())) {
                continue;
            }
            int missing = row.getMissingCount() == null ? 0 : row.getMissingCount();
            missing++;
            row.setMissingCount(missing);
            row.setUpdateTime(now);
            if (missing >= threshold) {
                row.setActiveFlag(0);
            }
            alertMapper.updateById(row);
        }
    }

    private List<SfWeatherAlert> listMatchedForCurrentTenant(boolean activeOnly) {
        WeatherAlertRegionMatcher.TenantRegion tenant = requireTenantRegion();
        List<SfWeatherAlert> source = activeOnly
            ? alertMapper.selectActiveAll()
            : alertMapper.selectList(Wrappers.lambdaQuery());
        List<SfWeatherAlert> matched = new ArrayList<>();
        for (SfWeatherAlert row : source) {
            if (row == null) {
                continue;
            }
            if (WeatherAlertRegionMatcher.matches(tenant, row.getProvinceCode(), row.getCityCode(), row.getDistrictCode())) {
                matched.add(row);
            }
        }
        return matched;
    }

    private WeatherAlertRegionMatcher.TenantRegion requireTenantRegion() {
        String tenantId = LoginHelper.getLoginUser() != null ? LoginHelper.getLoginUser().getTenantId() : null;
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("当前登录未关联租户，无法查询气象预警");
        }
        SfMasterTenantRegionAccessor.TenantCodes codes = tenantRegionAccessor.selectCodesByTenantId(tenantId.trim());
        if (codes == null || (StringUtils.isBlank(codes.getProvinceCode())
            && StringUtils.isBlank(codes.getCityCode())
            && StringUtils.isBlank(codes.getDistrictCode()))) {
            throw new ServiceException("租户未配置行政区划，无法查询气象预警");
        }
        return new WeatherAlertRegionMatcher.TenantRegion(
            codes.getProvinceCode(), codes.getCityCode(), codes.getDistrictCode());
    }

    private String resolveFreshnessStatus() {
        SfWeatherAlertSyncMeta success = syncMetaMapper.selectById(SfWeatherAlertSyncMeta.KEY_LAST_SUCCESS_AT);
        long count = alertMapper.selectCount(null);
        if (success == null || StringUtils.isBlank(success.getMetaValue())) {
            return count > 0 ? STALE : UNAVAILABLE;
        }
        try {
            Instant last = Instant.parse(success.getMetaValue().trim());
            long minutes = Math.max(1, properties.getFreshWithinMinutes());
            if (last.isAfter(Instant.now().minusSeconds(minutes * 60))) {
                return FRESH;
            }
            return count > 0 ? STALE : UNAVAILABLE;
        } catch (Exception ex) {
            return count > 0 ? STALE : UNAVAILABLE;
        }
    }

    private void writeMeta(String key, String value) {
        SfWeatherAlertSyncMeta existing = syncMetaMapper.selectById(key);
        Date now = new Date();
        if (existing == null) {
            SfWeatherAlertSyncMeta row = new SfWeatherAlertSyncMeta();
            row.setMetaKey(key);
            row.setMetaValue(value);
            row.setUpdateTime(now);
            syncMetaMapper.insert(row);
        } else {
            existing.setMetaValue(value);
            existing.setUpdateTime(now);
            syncMetaMapper.updateById(existing);
        }
    }

    private SfWeatherAlertVo toVo(SfWeatherAlert row, String freshness, boolean detail) {
        SfWeatherAlertVo vo = new SfWeatherAlertVo();
        vo.setWarningId(row.getWarningId());
        vo.setTitle(row.getTitle());
        vo.setHeadline(row.getHeadline());
        vo.setAlertTypeCode(row.getAlertTypeCode());
        vo.setAlertTypeName(row.getAlertTypeName());
        vo.setLevelCode(row.getLevelCode());
        vo.setLevelName(row.getLevelName());
        vo.setPublisher(row.getPublisher());
        vo.setEffectiveAt(formatOut(row.getEffectiveAt()));
        vo.setExpiresAt(formatOut(row.getExpiresAt()));
        vo.setAreaText(row.getAreaText());
        vo.setSourceUrl(row.getSourceUrl());
        vo.setFreshnessStatus(freshness);
        vo.setActive(Objects.equals(row.getActiveFlag(), 1));
        if (detail) {
            vo.setDescription(row.getDescription());
            vo.setInstructions(parseInstructions(row.getInstructionJson()));
        }
        return vo;
    }

    private static List<String> parseInstructions(String json) {
        if (StringUtils.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JSON.parseArray(json, String.class);
            return list != null ? list : new ArrayList<>();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String buildSourceUrl(String pathOrUrl) {
        if (StringUtils.isBlank(pathOrUrl)) {
            return null;
        }
        String raw = pathOrUrl.trim();
        if (raw.startsWith("http://") || raw.startsWith("https://")) {
            HttpUrl parsed = HttpUrl.parse(raw);
            if (parsed == null) {
                return null;
            }
            String host = parsed.host().toLowerCase(Locale.ROOT);
            if (!properties.getAllowedHost().equalsIgnoreCase(host)) {
                return null;
            }
            return parsed.newBuilder().scheme("https").build().toString();
        }
        String base = properties.getBaseUrl();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        if (raw.startsWith("/")) {
            raw = raw.substring(1);
        }
        return base + raw;
    }

    private static Date parseIssueTime(String raw) {
        if (StringUtils.isBlank(raw)) {
            return null;
        }
        String text = raw.trim();
        for (DateTimeFormatter fmt : IN_FMTS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(text, fmt);
                return Date.from(ldt.atZone(BEIJING).toInstant());
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    private static String formatOut(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), BEIJING).format(OUT_FMT);
    }
}
