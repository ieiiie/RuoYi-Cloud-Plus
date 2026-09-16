package com.ym.iot.jetlinks.service.impl;

import static com.ym.iot.jetlinks.client.JetLinksRpcClient.*;
import static com.ym.iot.jetlinks.support.JetLinksMapping.*;

import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksProductService;
import com.ym.iot.jetlinks.service.IJetLinksPropertyService;
import com.ym.iot.jetlinks.support.JetLinksAccess;
import com.ym.iot.product.domain.bo.IotProductPropertyBo;
import com.ym.iot.product.domain.vo.IotProductPropertyVo;
import com.ym.jetlinks.rpc.*;

import lombok.RequiredArgsConstructor;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Primary
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksPropertyServiceImpl implements IJetLinksPropertyService {
    private final JetLinksRpcClient rpc;
    private final IJetLinksProductService products;
    private final JetLinksAccess access;

    private List<RecordDto> records(Long productId) {
        access.ids();
        if (productId != null) return await(rpc.getCatalog().properties(productId.toString()));
        List<RecordDto> result = new ArrayList<>();
        for (RecordDto product : products.records(null))
            result.addAll(await(rpc.getCatalog().properties(product.id())));
        return result;
    }

    private RecordDto get(Long id) {
        if (id == null) throw new ServiceException("属性ID不能为空");
        return records(null).stream()
                .filter(r -> id.toString().equals(r.id()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<IotProductPropertyVo> queryList(IotProductPropertyBo bo) {
        Map<String, Object> filters =
                filters(bo, "propertyId", "identifier", "name", "dataType", "rwFlag");
        return beans(
                records(bo == null ? null : bo.getProductId()).stream()
                        .filter(
                                r ->
                                        filters.entrySet().stream()
                                                .allMatch(
                                                        e -> {
                                                            Object v =
                                                                    "propertyId".equals(e.getKey())
                                                                            ? r.id()
                                                                            : r.data()
                                                                                    .get(
                                                                                            e
                                                                                                    .getKey());
                                                            return v != null
                                                                    && (("name".equals(e.getKey())
                                                                                    || "identifier"
                                                                                            .equals(
                                                                                                    e
                                                                                                            .getKey()))
                                                                            ? v.toString()
                                                                                    .contains(
                                                                                            e.getValue()
                                                                                                    .toString())
                                                                            : v.toString()
                                                                                    .equals(
                                                                                            e.getValue()
                                                                                                    .toString()));
                                                        }))
                        .toList(),
                "propertyId",
                IotProductPropertyVo.class);
    }

    @Override
    public PageResult<IotProductPropertyVo> queryPageList(IotProductPropertyBo bo, PageQuery page) {
        return slice(queryList(bo), page);
    }

    @Override
    public IotProductPropertyVo queryById(Long id) {
        return bean(get(id), "propertyId", IotProductPropertyVo.class);
    }
}
