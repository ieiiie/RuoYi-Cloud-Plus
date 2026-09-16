package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;
import static com.ym.iot.jetlinks.support.JetLinksMapping.*;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksProductService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.jetlinks.support.JetLinksMapping;
import com.ym.iot.product.domain.bo.IotProductBo;
import com.ym.iot.product.domain.vo.IotProductExportVo;
import com.ym.iot.product.domain.vo.IotProductVo;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksProductServiceImpl implements IJetLinksProductService {
    private final JetLinksRpcClient rpc;
    private final JetLinksAccess access;

    private QueryDto query(IotProductBo bo, PageQuery page) {
        access.ids(); // requires an authenticated tenant even for the global core catalog
        return JetLinksMapping.query(
                bo != null && bo.getProductId() != null
                        ? List.of(bo.getProductId().toString())
                        : null,
                filters(
                        bo,
                        "productKey",
                        "productName",
                        "deviceCategory",
                        "status",
                        "protocol",
                        "nodeType",
                        "netType",
                        "dataFormat"),
                page);
    }

    public List<RecordDto> records(IotProductBo bo) {
        return all(rpc.getCatalog()::products, query(bo, null));
    }

    @Override
    public List<IotProductVo> queryList(IotProductBo bo) {
        return beans(records(bo), "productId", IotProductVo.class);
    }

    @Override
    public PageResult<IotProductVo> queryPageList(IotProductBo bo, PageQuery page) {
        if (page == null || page.getPageSize() == null || page.getPageSize() > 500)
            return slice(queryList(bo), page);
        return page(
                await(rpc.getCatalog().products(query(bo, page))), "productId", IotProductVo.class);
    }

    public RecordDto get(Long id) {
        access.ids();
        if (id == null) throw new ServiceException("产品ID不能为空");
        return await(rpc.getCatalog().product(id.toString()));
    }

    @Override
    public IotProductVo queryById(Long id) {
        return bean(get(id), "productId", IotProductVo.class);
    }

    @Override
    public List<IotProductExportVo> queryExportList(IotProductBo bo) {
        Map<String, String> names = new LinkedHashMap<>();
        for (var category : await(rpc.getCatalog().categories()))
            names.put(category.code(), category.name());
        List<IotProductExportVo> rows = beans(records(bo), "productId", IotProductExportVo.class);
        for (var row : rows)
            if (names.containsKey(row.getDeviceCategory()))
                row.setDeviceCategory(names.get(row.getDeviceCategory()));
        return rows;
    }
}
