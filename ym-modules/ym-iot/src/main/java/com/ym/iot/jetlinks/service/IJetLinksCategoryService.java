package com.ym.iot.jetlinks.service;

import com.ym.jetlinks.rpc.CategoryDto;

import java.util.List;

/** 产品分类以 JetLinks 原生目录为准。 */
public interface IJetLinksCategoryService {
    List<CategoryDto> listCategories();
}
