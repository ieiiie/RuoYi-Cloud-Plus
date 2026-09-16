package com.ym.agriculture.farming.weather.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.agriculture.farming.field.support.SfMasterTenantDistrictAccessor;
import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.farming.weather.config.AmapWeatherProperties;
import com.ym.agriculture.farming.weather.event.SfWeatherI18nSnapshotCommittedEvent;
import com.ym.agriculture.farming.weather.dao.SfWeatherForecastMapper;
import com.ym.agriculture.farming.weather.dao.SfWeatherLiveMapper;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherForecast;
import com.ym.agriculture.farming.weather.model.entity.SfWeatherLive;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherForecastVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLatestVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherLiveVo;
import com.ym.agriculture.farming.weather.model.vo.SfWeatherSyncResultVo;
import com.ym.agriculture.farming.weather.remote.AmapWeatherApi;
import com.ym.agriculture.farming.weather.remote.dto.AmapWeatherCast;
import com.ym.agriculture.farming.weather.remote.dto.AmapWeatherForecastBlock;
import com.ym.agriculture.farming.weather.remote.dto.AmapWeatherInfoResponse;
import com.ym.agriculture.farming.weather.remote.dto.AmapWeatherLive;
import com.ym.agriculture.farming.weather.service.ISfWeatherService;
import com.ym.agriculture.farming.weather.support.LiveCacheBoundarySupport;
import com.ym.agriculture.farming.weather.support.SfWeatherI18nSourceFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Response;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 高德天气：预报由定时任务 {@code extensions=all} 同步；实况查询时按日时段边界按需 {@code extensions=base} 拉取。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfWeatherServiceImpl implements ISfWeatherService {

    private static final String EXT_ALL = "all";
    private static final String EXT_BASE = "base";
    private static final String OUTPUT_JSON = "JSON";

    private final AmapWeatherProperties properties;
    private final SfWeatherForecastMapper forecastMapper;
    private final SfWeatherLiveMapper liveMapper;
    private final ObjectProvider<AmapWeatherApi> amapWeatherApiProvider;
    private final SfMasterTenantDistrictAccessor tenantDistrictAccessor;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfWeatherSyncResultVo syncFromAmap(String adcode) {
        assertAmapConfigured();
        if (StringUtils.isBlank(adcode)) {
            throw new ServiceException("adcode 不能为空");
        }
        String ad = adcode.trim();
        long syncId = IdWorker.getId();
        Date pullTime = new Date();
        int forecastRows = fetchAndSaveForecast(ad, syncId, pullTime);

        SfWeatherSyncResultVo vo = new SfWeatherSyncResultVo();
        vo.setSyncId(syncId);
        vo.setAdcode(ad);
        vo.setForecastRows(forecastRows);
        return vo;
    }

    @Override
    public SfWeatherLatestVo getLatest() {
        String tenantId = LoginHelper.getLoginUser() != null ? LoginHelper.getLoginUser().getTenantId() : null;
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("当前登录未关联租户，无法查询天气");
        }
        String ad = tenantDistrictAccessor.selectNormalizedDistrictCode(tenantId.trim());
        if (StringUtils.isBlank(ad)) {
            log.error("租户未配置区县行政区划编码(district_code)，跳过天气查询，tenantId={}", tenantId.trim());
            return new SfWeatherLatestVo();
        }
        SfWeatherLatestVo out = new SfWeatherLatestVo();
        out.setAdcode(ad);
        out.setForecasts(buildForecastsFromDb(ad));
        out.setLive(getLiveByAdcode(ad));
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SfWeatherLiveVo getLiveByAdcode(String adcode) {
        assertAmapConfigured();
        if (StringUtils.isBlank(adcode)) {
            throw new ServiceException("adcode 不能为空");
        }
        String ad = adcode.trim();
        Date now = new Date();
        Date boundary = LiveCacheBoundarySupport.resolveBoundary(now, properties.getLiveRefreshHours());

        SfWeatherLive cached = liveMapper.selectLatestOneByAdcode(ad);
        if (cached != null && cached.getPullTime() != null && !cached.getPullTime().before(boundary)) {
            return toLiveVo(cached);
        }
        SfWeatherLive saved = fetchAndSaveLive(ad, now);
        return saved != null ? toLiveVo(saved) : null;
    }

    private List<SfWeatherForecastVo> buildForecastsFromDb(String adcode) {
        String today = todayString();
        List<SfWeatherForecast> merged = new ArrayList<>();
        merged.addAll(forecastMapper.selectListByAdcodeAndCastDateBeforeOrderByCastDateAsc(adcode, today));

        SfWeatherForecast latestOne = forecastMapper.selectLatestOneByAdcode(adcode);
        if (latestOne != null) {
            List<SfWeatherForecast> currentRows =
                forecastMapper.selectListBySyncIdOrderByCastDateAsc(latestOne.getSyncId());
            for (SfWeatherForecast row : currentRows) {
                if (row.getCastDate() != null && row.getCastDate().compareTo(today) >= 0) {
                    merged.add(row);
                }
            }
        }

        if (merged.isEmpty()) {
            return List.of();
        }
        merged.sort(Comparator.comparing(SfWeatherForecast::getCastDate, Comparator.nullsLast(String::compareTo)));
        List<SfWeatherForecastVo> vos = new ArrayList<>(merged.size());
        for (SfWeatherForecast row : merged) {
            vos.add(toForecastVo(row));
        }
        return vos;
    }

    private SfWeatherLive fetchAndSaveLive(String adcode, Date pullTime) {
        AmapWeatherInfoResponse resp = callAmap(adcode, EXT_BASE);
        if (resp.getLives() == null || resp.getLives().isEmpty()) {
            log.warn("高德实况返回无 lives，adcode={}", adcode);
            return null;
        }
        AmapWeatherLive live = resp.getLives().get(0);
        if (live == null) {
            return null;
        }
        String blockAdcode = nz(live.getAdcode());
        if (blockAdcode == null) {
            blockAdcode = adcode;
        }
        SfWeatherLive entity = new SfWeatherLive();
        entity.setAdcode(blockAdcode);
        entity.setProvince(nz(live.getProvince()));
        entity.setCityName(nz(live.getCity()));
        entity.setWeather(nz(live.getWeather()));
        entity.setTemperature(nz(live.getTemperature()));
        entity.setWindDirection(nz(live.getWindDirection()));
        entity.setWindPower(nz(live.getWindPower()));
        entity.setHumidity(nz(live.getHumidity()));
        entity.setReportTime(nz(live.getReportTime()));
        entity.setPullTime(pullTime);
        liveMapper.insert(entity);
        eventPublisher.publishEvent(new SfWeatherI18nSnapshotCommittedEvent(blockAdcode,
            SfWeatherI18nSourceFactory.sources(entity)));
        return entity;
    }

    private int fetchAndSaveForecast(String adcode, long syncId, Date pullTime) {
        AmapWeatherInfoResponse resp = callAmap(adcode, EXT_ALL);
        if (resp.getForecasts() == null || resp.getForecasts().isEmpty()) {
            log.warn("高德预报返回无 forecasts，adcode={}", adcode);
            return 0;
        }
        AmapWeatherForecastBlock block = resp.getForecasts().get(0);
        String province = nz(block.getProvince());
        String cityName = nz(block.getCity());
        String reportTime = nz(block.getReportTime());
        String blockAdcode = nz(block.getAdcode());
        if (blockAdcode == null) {
            blockAdcode = adcode;
        }
        List<AmapWeatherCast> casts = block.getCasts();
        if (casts == null || casts.isEmpty()) {
            return 0;
        }
        String today = todayString();
        forecastMapper.deleteByAdcodeAndCastDateFrom(blockAdcode, today);
        int c = 0;
        List<SfWeatherForecast> inserted = new ArrayList<>();
        for (AmapWeatherCast cast : casts) {
            if (cast == null) {
                continue;
            }
            SfWeatherForecast e = new SfWeatherForecast();
            e.setSyncId(syncId);
            e.setAdcode(blockAdcode);
            e.setProvince(province);
            e.setCityName(cityName);
            e.setForecastReportTime(reportTime);
            e.setCastDate(nz(cast.getDate()));
            e.setWeekNum(nz(cast.getWeek()));
            e.setDayWeather(nz(cast.getDayWeather()));
            e.setNightWeather(nz(cast.getNightWeather()));
            e.setDayTemp(nz(cast.getDayTemp()));
            e.setNightTemp(nz(cast.getNightTemp()));
            e.setDayWind(nz(cast.getDayWind()));
            e.setNightWind(nz(cast.getNightWind()));
            e.setDayPower(nz(cast.getDayPower()));
            e.setNightPower(nz(cast.getNightPower()));
            e.setDayTempFloat(nz(cast.getDayTempFloat()));
            e.setNightTempFloat(nz(cast.getNightTempFloat()));
            e.setPullTime(pullTime);
            forecastMapper.insert(e);
            inserted.add(e);
            c++;
        }
        if (!inserted.isEmpty()) {
            List<I18nTextSource> sources = inserted.stream()
                .flatMap(row -> SfWeatherI18nSourceFactory.sources(row).stream())
                .toList();
            eventPublisher.publishEvent(new SfWeatherI18nSnapshotCommittedEvent(blockAdcode, sources));
        }
        return c;
    }

    private void assertAmapConfigured() {
        if (!properties.isEnabled()) {
            throw new ServiceException("高德天气功能未启用（amap-weather.enabled=false）");
        }
        if (StringUtils.isBlank(properties.getKey())) {
            throw new ServiceException("未配置高德 Web 服务 Key（amap-weather.key）");
        }
    }

    private AmapWeatherInfoResponse callAmap(String adcode, String extensions) {
        AmapWeatherApi api = amapWeatherApiProvider.getIfAvailable();
        if (api == null) {
            throw new ServiceException("高德天气 Retrofit 未注册（请确认 amap-weather.enabled=true）");
        }
        try {
            Response<AmapWeatherInfoResponse> response = api.weatherInfo(
                properties.getKey().trim(),
                adcode,
                extensions,
                OUTPUT_JSON
            ).execute();
            if (!response.isSuccessful()) {
                throw new ServiceException("高德天气 HTTP 状态异常: " + response.code());
            }
            AmapWeatherInfoResponse body = response.body();
            if (body == null) {
                throw new ServiceException("高德天气响应体为空");
            }
            if (!"1".equals(body.getStatus())) {
                throw new ServiceException("高德天气接口失败: " + body.getInfo());
            }
            if (!"10000".equals(body.getInfocode())) {
                throw new ServiceException("高德天气 infocode: " + body.getInfocode());
            }
            return body;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("调用高德天气异常 adcode={} extensions={}", adcode, extensions, e);
            throw new ServiceException("调用高德天气异常: " + e.getMessage());
        }
    }

    private static String nz(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String todayString() {
        return DateUtil.format(new Date(), "yyyy-MM-dd");
    }

    private static SfWeatherForecastVo toForecastVo(SfWeatherForecast e) {
        SfWeatherForecastVo v = new SfWeatherForecastVo();
        BeanUtil.copyProperties(e, v);
        return v;
    }

    private static SfWeatherLiveVo toLiveVo(SfWeatherLive e) {
        SfWeatherLiveVo v = new SfWeatherLiveVo();
        BeanUtil.copyProperties(e, v);
        return v;
    }
}
