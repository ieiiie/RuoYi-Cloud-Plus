package com.ym.agriculture.farming.solarterms.service.impl;

import com.alibaba.fastjson2.JSON;
import com.nlf.calendar.Lunar;
import com.nlf.calendar.Solar;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.farming.solarterms.dao.SfSolarTermDefinitionMapper;
import com.ym.agriculture.farming.solarterms.dao.SfSolarTermOccurrenceMapper;
import com.ym.agriculture.farming.solarterms.model.entity.SfSolarTermDefinition;
import com.ym.agriculture.farming.solarterms.model.entity.SfSolarTermOccurrence;
import com.ym.agriculture.farming.solarterms.model.vo.CalendarDateInfoVo;
import com.ym.agriculture.farming.solarterms.model.vo.SfSolarTermDetailVo;
import com.ym.agriculture.farming.solarterms.model.vo.SfSolarTermSummaryVo;
import com.ym.agriculture.farming.solarterms.service.ISfSolarTermService;
import com.ym.agriculture.farming.solarterms.support.SolarTermCalculator;
import com.ym.agriculture.farming.solarterms.support.SolarTermCodes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 节气服务：算法生成交节时间 + 平台定义内容。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfSolarTermServiceImpl implements ISfSolarTermService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId BEIJING = SolarTermCalculator.BEIJING;

    private final SfSolarTermDefinitionMapper definitionMapper;
    private final SfSolarTermOccurrenceMapper occurrenceMapper;

    @Override
    public SfSolarTermDetailVo getCurrent() {
        LocalDateTime now = LocalDateTime.now(BEIJING);
        ensureYears(now.getYear());
        Date moment = SolarTermCalculator.toDate(now);
        SfSolarTermOccurrence current = occurrenceMapper.selectLatestOnOrBefore(moment);
        if (current == null) {
            throw new ServiceException("暂无可用的节气数据");
        }
        SfSolarTermOccurrence next = occurrenceMapper.selectEarliestAfter(current.getOccurredAt());
        return toDetail(current, next);
    }

    @Override
    public SfSolarTermDetailVo getByTermCode(String termCode, Integer year) {
        if (StringUtils.isBlank(termCode)) {
            throw new ServiceException("节气编码不能为空");
        }
        String code = termCode.trim().toLowerCase();
        if (SolarTermCodes.nameOfCode(code) == null) {
            throw new ServiceException("未知的节气编码: " + termCode);
        }
        int y = year != null ? year : LocalDate.now(BEIJING).getYear();
        ensureYears(y);
        SfSolarTermOccurrence occurrence = occurrenceMapper.selectByYearAndCode(y, code);
        if (occurrence == null) {
            throw new ServiceException("未找到指定年份的节气数据");
        }
        SfSolarTermOccurrence next = occurrenceMapper.selectEarliestAfter(occurrence.getOccurredAt());
        return toDetail(occurrence, next);
    }

    @Override
    public CalendarDateInfoVo getCalendarDateInfo(Date happenedAt) {
        if (happenedAt == null) {
            throw new ServiceException("发生时间不能为空");
        }
        LocalDateTime selectedAt = LocalDateTime.ofInstant(happenedAt.toInstant(), BEIJING);
        ensureYears(selectedAt.getYear());
        SfSolarTermOccurrence current = occurrenceMapper.selectLatestOnOrBefore(happenedAt);
        if (current == null) {
            throw new ServiceException("暂无可用的节气数据");
        }
        Lunar lunar;
        try {
            Solar solar = Solar.fromYmdHms(
                selectedAt.getYear(), selectedAt.getMonthValue(), selectedAt.getDayOfMonth(),
                selectedAt.getHour(), selectedAt.getMinute(), selectedAt.getSecond());
            lunar = solar.getLunar();
        } catch (RuntimeException ex) {
            throw new ServiceException("发生时间超出农历计算范围");
        }
        CalendarDateInfoVo vo = new CalendarDateInfoVo();
        vo.setGregorianDateTime(selectedAt.format(DATE_TIME));
        vo.setLunarDateText(lunar.getYearInChinese() + "年" + lunar.getMonthInChinese() + "月" + lunar.getDayInChinese());
        vo.setSolarTermCode(current.getTermCode());
        vo.setSolarTermName(current.getTermName());
        vo.setSolarTermOccurredAt(formatDateTime(current.getOccurredAt()));
        vo.setTimeZone(BEIJING.getId());
        vo.setAlgorithmVersion(SolarTermCodes.ALGORITHM_VERSION);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ensureYears(int centerYear) {
        for (int y = centerYear - 1; y <= centerYear + 1; y++) {
            ensureSingleYear(y);
        }
    }

    private void ensureSingleYear(int year) {
        if (occurrenceMapper.countByYear(year) >= 24) {
            return;
        }
        List<SolarTermCalculator.TermInstant> instants = SolarTermCalculator.calculateYear(year);
        Date now = new Date();
        for (SolarTermCalculator.TermInstant instant : instants) {
            if (occurrenceMapper.selectByYearAndCode(year, instant.getTermCode()) != null) {
                continue;
            }
            SfSolarTermOccurrence row = new SfSolarTermOccurrence();
            row.setTermYear(year);
            row.setTermCode(instant.getTermCode());
            row.setTermName(instant.getTermName());
            row.setOccurredAt(SolarTermCalculator.toDate(instant.getOccurredAt()));
            row.setGregorianDate(SolarTermCalculator.toDate(instant.getOccurredAt().toLocalDate().atStartOfDay()));
            row.setLunarDateText(instant.getLunarDateText());
            row.setAlgorithmVersion(SolarTermCodes.ALGORITHM_VERSION);
            row.setCreateTime(now);
            row.setUpdateTime(now);
            try {
                occurrenceMapper.insert(row);
            } catch (DuplicateKeyException ex) {
                log.debug("节气交节数据已存在 year={} code={}", year, instant.getTermCode());
            }
        }
    }

    private SfSolarTermDetailVo toDetail(SfSolarTermOccurrence occurrence, SfSolarTermOccurrence next) {
        SfSolarTermDefinition def = definitionMapper.selectEnabledByCode(occurrence.getTermCode());
        SfSolarTermDetailVo vo = new SfSolarTermDetailVo();
        vo.setTermCode(occurrence.getTermCode());
        vo.setTermName(occurrence.getTermName());
        vo.setTermYear(occurrence.getTermYear());
        vo.setOccurredAt(formatDateTime(occurrence.getOccurredAt()));
        vo.setGregorianDate(formatDate(occurrence.getGregorianDate()));
        vo.setLunarDateText(occurrence.getLunarDateText());
        if (def != null) {
            vo.setTermOrder(def.getTermOrder());
            vo.setIntro(def.getIntro());
            vo.setSeasonalDescription(def.getSeasonalDescription());
            vo.setCustoms(parseCustoms(def.getCustomsJson()));
        } else {
            vo.setIntro("");
            vo.setSeasonalDescription("");
            vo.setCustoms(new ArrayList<>());
        }
        if (next != null) {
            SfSolarTermSummaryVo summary = new SfSolarTermSummaryVo();
            summary.setTermCode(next.getTermCode());
            summary.setTermName(next.getTermName());
            summary.setOccurredAt(formatDateTime(next.getOccurredAt()));
            summary.setGregorianDate(formatDate(next.getGregorianDate()));
            vo.setNextTerm(summary);
        }
        return vo;
    }

    private static List<String> parseCustoms(String customsJson) {
        if (StringUtils.isBlank(customsJson)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = JSON.parseArray(customsJson, String.class);
            return list != null ? list : new ArrayList<>();
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), BEIJING).format(DATE_TIME);
    }

    private static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return LocalDateTime.ofInstant(date.toInstant(), BEIJING).toLocalDate().format(DATE);
    }
}
