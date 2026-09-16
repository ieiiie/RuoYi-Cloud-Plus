package com.ym.iot.product.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.product.domain.bo.IotProductBo;
import com.ym.iot.product.domain.vo.IotProductExportVo;
import com.ym.iot.product.domain.vo.IotProductVo;

import java.util.List;

/**
 * 物模型产品业务接口。
 * <p>
 * 物模型产品定义设备类型（如传感器、网关），包含 productKey、productName、deviceCategory 等。
 * 状态：0 开发中，1 已发布。此接口仅供读取。
 * </p>
 *
 * @author ym-cloud
 */
public interface IIotProductService {

    /**
     * 按条件查询产品列表，不分页。
     *
     * @param bo 查询条件
     * @return 产品列表
     */
    List<IotProductVo> queryList(IotProductBo bo);

    /**
     * 分页查询产品列表。
     */
    PageResult<IotProductVo> queryPageList(IotProductBo bo, PageQuery pageQuery);

    /**
     * 根据主键查询产品详情。
     */
    IotProductVo queryById(Long productId);

    /**
     * 查询导出用列表。
     */
    List<IotProductExportVo> queryExportList(IotProductBo bo);
}
