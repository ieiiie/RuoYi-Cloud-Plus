package com.ym.agriculture.farming.solarterms.dao;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ym.agriculture.farming.solarterms.model.entity.SfSolarTermOccurrence;

import java.util.Date;
import java.util.List;

/** {@code sf_solar_term_occurrence} Mapper。 */
@InterceptorIgnore(tenantLine = "true")
public interface SfSolarTermOccurrenceMapper extends BaseMapper<SfSolarTermOccurrence> {

    default int countByYear(int termYear) {
        return Math.toIntExact(selectCount(Wrappers.<SfSolarTermOccurrence>lambdaQuery()
            .eq(SfSolarTermOccurrence::getTermYear, termYear)));
    }

    default SfSolarTermOccurrence selectByYearAndCode(int termYear, String termCode) {
        return selectOne(Wrappers.<SfSolarTermOccurrence>lambdaQuery()
            .eq(SfSolarTermOccurrence::getTermYear, termYear)
            .eq(SfSolarTermOccurrence::getTermCode, termCode)
            .last("LIMIT 1"));
    }

    default SfSolarTermOccurrence selectLatestOnOrBefore(Date moment) {
        return selectOne(Wrappers.<SfSolarTermOccurrence>lambdaQuery()
            .le(SfSolarTermOccurrence::getOccurredAt, moment)
            .orderByDesc(SfSolarTermOccurrence::getOccurredAt)
            .last("LIMIT 1"));
    }

    default SfSolarTermOccurrence selectEarliestAfter(Date moment) {
        return selectOne(Wrappers.<SfSolarTermOccurrence>lambdaQuery()
            .gt(SfSolarTermOccurrence::getOccurredAt, moment)
            .orderByAsc(SfSolarTermOccurrence::getOccurredAt)
            .last("LIMIT 1"));
    }

    default List<SfSolarTermOccurrence> selectByYear(int termYear) {
        return selectList(Wrappers.<SfSolarTermOccurrence>lambdaQuery()
            .eq(SfSolarTermOccurrence::getTermYear, termYear)
            .orderByAsc(SfSolarTermOccurrence::getOccurredAt));
    }
}
