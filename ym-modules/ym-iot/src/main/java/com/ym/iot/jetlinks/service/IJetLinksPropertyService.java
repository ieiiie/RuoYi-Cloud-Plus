package com.ym.iot.jetlinks.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.product.domain.bo.IotProductPropertyBo;
import com.ym.iot.product.domain.vo.IotProductPropertyVo;
import com.ym.iot.product.service.IIotProductPropertyService;

import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksPropertyService extends IIotProductPropertyService {
    List<IotProductPropertyVo> queryList(IotProductPropertyBo bo);

    PageResult<IotProductPropertyVo> queryPageList(IotProductPropertyBo bo, PageQuery page);

    IotProductPropertyVo queryById(Long id);
}
