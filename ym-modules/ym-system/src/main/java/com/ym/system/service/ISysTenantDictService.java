package com.ym.system.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.domain.bo.SysTenantDictDataBo;
import com.ym.system.domain.bo.SysTenantDictTypeBo;
import com.ym.system.domain.vo.SysTenantDictDataVo;
import com.ym.system.domain.vo.SysTenantDictTypeVo;

import java.util.Collection;
import java.util.List;

public interface ISysTenantDictService {
    PageResult<SysTenantDictTypeVo> queryTypePage(SysTenantDictTypeBo bo, PageQuery pageQuery);
    List<SysTenantDictTypeVo> queryTypeOptions();
    SysTenantDictTypeVo queryType(Long id);
    PageResult<SysTenantDictDataVo> queryDataPage(SysTenantDictDataBo bo, PageQuery pageQuery);
    List<SysTenantDictDataVo> queryDataByType(String dictType);
    SysTenantDictDataVo queryData(Long id);
    Long insertData(SysTenantDictDataBo bo);
    void updateData(SysTenantDictDataBo bo);
    void sortData(List<Long> dictCodes);
    void deleteData(Collection<Long> ids);
}
