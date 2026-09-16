package com.ym.iot.jetlinks.service.impl;

import com.ym.common.core.exception.ServiceException;
import com.ym.iot.jetlinks.client.JetLinksRpcClient;
import com.ym.iot.jetlinks.config.ConditionalOnJetLinks;
import com.ym.iot.jetlinks.service.IJetLinksCategoryService;
import com.ym.jetlinks.rpc.CategoryDto;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

import java.util.List;

/** 读取平台分类目录；上游失败明确报错，不能伪装成“暂无分类”。 */
@Service
@ConditionalOnJetLinks
@RequiredArgsConstructor
public class JetLinksCategoryServiceImpl implements IJetLinksCategoryService {
    private final JetLinksRpcClient rpc;

    @Override
    public List<CategoryDto> listCategories() {
        var categories = JetLinksRpcClient.await(rpc.getCatalog().categories());
        if (categories == null) throw new ServiceException("核心未返回产品分类");
        return categories;
    }
}
