package com.ym.agriculture.farming.crop.service;

import com.ym.agriculture.farming.crop.model.bo.SfCropVarietyBo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyExportVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropVarietyVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 作物品种服务。
 */
public interface ISfCropVarietyService {

    List<SfCropVarietyVo> queryList(SfCropVarietyBo bo);

    List<SfCropVarietyVo> queryListBySpeciesId(Long speciesId);

    PageResult<SfCropVarietyVo> queryPageList(SfCropVarietyBo bo, PageQuery pageQuery);

    SfCropVarietyVo queryById(Long varietyId);

    Boolean insertByBo(SfCropVarietyBo bo);

    Boolean updateByBo(SfCropVarietyBo bo);

    Boolean updateStatus(Long varietyId, String status);

    Boolean deleteWithValidByIds(Collection<Long> varietyIds);

    List<SfCropVarietyExportVo> queryExportList(SfCropVarietyBo bo);
}
