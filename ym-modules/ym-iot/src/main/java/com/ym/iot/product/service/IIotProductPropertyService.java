package com.ym.iot.product.service;

import com.ym.common.core.domain.PageResult;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.iot.product.domain.bo.IotProductPropertyBo;
import com.ym.iot.product.domain.vo.IotProductPropertyVo;

import java.util.List;

/**
 * 物模型属性业务接口。
 * <p>
 * 管理物模型产品下的属性（如温度、湿度），identifier 在同产品内唯一。
 * 数据类型支持 INT/FLOAT/STRING 等，读写权限 R/W。
 * </p>
 *
 * @author ym-cloud
 */
public interface IIotProductPropertyService {

    /** 按条件查询属性列表，不分页。 */
    List<IotProductPropertyVo> queryList(IotProductPropertyBo bo);

    /** 分页查询属性列表。 */
    PageResult<IotProductPropertyVo> queryPageList(IotProductPropertyBo bo, PageQuery pageQuery);

    /** 根据主键查询属性详情。 */
    IotProductPropertyVo queryById(Long propertyId);

}
