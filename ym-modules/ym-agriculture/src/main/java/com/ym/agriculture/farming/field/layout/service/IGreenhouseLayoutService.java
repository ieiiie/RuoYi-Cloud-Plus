package com.ym.agriculture.farming.field.layout.service;

import com.ym.agriculture.farming.field.layout.model.bo.GreenhouseLayoutDraftBo;
import com.ym.agriculture.farming.field.layout.model.vo.GreenhouseLayoutVo;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;

import java.util.Collection;
import java.util.List;

/**
 * 租户大棚二维布局服务。
 */
public interface IGreenhouseLayoutService {

    GreenhouseLayoutVo getLayout();

    GreenhouseLayoutVo resolveLayout(List<SfFieldVo> greenhouses);

    void saveLayout(GreenhouseLayoutDraftBo draft, Long currentFieldId);

    void removeFields(Collection<Long> fieldIds);
}
