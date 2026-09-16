package com.ym.agriculture.farming.crop.service;

import com.ym.agriculture.farming.crop.model.bo.SfCropSpeciesBo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesDetailVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesExportVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesTreeVo;
import com.ym.agriculture.farming.crop.model.vo.SfCropSpeciesVo;
import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 作物品类服务。
 */
public interface ISfCropSpeciesService {

    List<SfCropSpeciesVo> queryList(SfCropSpeciesBo bo);

    List<SfCropSpeciesTreeVo> queryTree(SfCropSpeciesBo bo, String varietyStatus);

    PageResult<SfCropSpeciesVo> queryPageList(SfCropSpeciesBo bo, PageQuery pageQuery);

    SfCropSpeciesVo queryById(Long speciesId);

    SfCropSpeciesDetailVo queryDetailById(Long speciesId);

    Boolean insertByBo(SfCropSpeciesBo bo);

    Boolean updateByBo(SfCropSpeciesBo bo);

    Boolean updateStatus(Long speciesId, String status);

    Boolean deleteWithValidByIds(Collection<Long> speciesIds);

    List<SfCropSpeciesExportVo> queryExportList(SfCropSpeciesBo bo);
}
