package com.ym.iot.jetlinks.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.jetlinks.domain.dto.CategoryProjection;
import com.ym.iot.product.domain.IotProduct;
import com.ym.iot.product.domain.vo.IotProductVo;

import org.apache.ibatis.annotations.*;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 物理业务库访问。全局设备目录、历史归属与任务查询使用显式业务条件， 不继承调用方租户拦截条件；访问权限在服务层校验，SQL 值使用绑定参数。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface JetLinksCatalogMapper extends BaseMapperPlus<IotProduct, IotProductVo> {
    @Select(
            """
            SELECT COUNT(*)
                    FROM iot_device
                    WHERE device_id=#{deviceId}
            """)
    Integer countDevice(@Param("deviceId") Long deviceId);

    @Update(
            """
            UPDATE iot_device
                    SET online_status=#{status}
                    WHERE device_id=#{deviceId}
            """)
    int updateDeviceOnline(@Param("status") String status, @Param("deviceId") Long deviceId);

    @Update(
            """
            UPDATE iot_product
                    SET del_flag='2'
                    WHERE product_id=#{productId}
            """)
    int archiveProduct(@Param("productId") Long productId);

    @Update(
            """
            UPDATE iot_device
                    SET device_category=#{category}
                    WHERE product_id=#{productId}
            """)
    int updateDevicesCategory(
            @Param("category") Object category, @Param("productId") Long productId);

    @Select(
            """
            SELECT code
                    FROM iot_jetlinks_category
                    WHERE category_id=#{categoryId}
                    FOR UPDATE
            """)
    List<String> lockCategoryCode(@Param("categoryId") String categoryId);

    @Insert(
            """
            INSERT INTO iot_jetlinks_category(code,name,parent_id,sort_index,archived,source_time,version,category_id)
                    VALUES(#{category.code},#{category.name},#{category.parentId},#{category.sortIndex},#{category.archived},#{category.sourceTime},#{category.version},#{category.categoryId})
            """)
    int insertCategory(@Param("category") CategoryProjection category);

    @Update(
            """
            UPDATE iot_jetlinks_category
                    SET code=#{category.code},name=#{category.name},parent_id=#{category.parentId},sort_index=#{category.sortIndex},archived=#{category.archived},source_time=#{category.sourceTime},version=#{category.version}
                    WHERE category_id=#{category.categoryId}
            """)
    int updateCategory(@Param("category") CategoryProjection category);

    @Update(
            """
            UPDATE iot_product
                    SET device_category=#{category}
                    WHERE device_category=#{oldCategory}
            """)
    int renameProductCategory(
            @Param("category") String category, @Param("oldCategory") String oldCategory);

    @Update(
            """
            UPDATE iot_device
                    SET device_category=#{category}
                    WHERE device_category=#{oldCategory}
            """)
    int renameDeviceCategory(
            @Param("category") String category, @Param("oldCategory") String oldCategory);
}
