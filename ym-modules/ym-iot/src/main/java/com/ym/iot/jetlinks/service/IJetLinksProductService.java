package com.ym.iot.jetlinks.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.product.domain.bo.IotProductBo;
import com.ym.iot.product.domain.vo.IotProductExportVo;
import com.ym.iot.product.domain.vo.IotProductVo;
import com.ym.iot.product.service.IIotProductService;
import com.ym.jetlinks.rpc.RecordDto;

import java.util.List;

/** JetLinks 业务接口；控制器和 Dubbo 适配只依赖业务契约。 */
public interface IJetLinksProductService extends IIotProductService {
    List<RecordDto> records(IotProductBo bo);

    List<IotProductVo> queryList(IotProductBo bo);

    PageResult<IotProductVo> queryPageList(IotProductBo bo, PageQuery page);

    RecordDto get(Long id);

    IotProductVo queryById(Long id);

    List<IotProductExportVo> queryExportList(IotProductBo bo);
}
