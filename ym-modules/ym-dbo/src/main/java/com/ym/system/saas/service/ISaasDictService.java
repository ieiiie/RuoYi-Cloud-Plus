package com.ym.system.saas.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.system.saas.domain.bo.*;
import com.ym.system.saas.domain.vo.*;

import java.util.Collection;

/**
 * SaaS 全局字典运营服务。
 */
public interface ISaasDictService {
    PageResult<SaasDictTypeVo> queryTypePage(SaasDictTypeBo bo, PageQuery pageQuery);

    SaasDictTypeVo queryType(Long id);

    Long insertType(SaasDictTypeBo bo);

    void updateType(SaasDictTypeBo bo);

    void deleteTypes(Collection<Long> ids);

    PageResult<SaasDictDataVo> queryDataPage(SaasDictDataBo bo, PageQuery pageQuery);

    SaasDictDataVo queryData(Long id);

    Long insertData(SaasDictDataBo bo);

    void updateData(SaasDictDataBo bo);

    void deleteData(Collection<Long> ids);
}
