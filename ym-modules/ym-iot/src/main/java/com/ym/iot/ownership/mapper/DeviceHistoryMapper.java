package com.ym.iot.ownership.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.mybatis.core.mapper.BaseMapperPlus;
import com.ym.iot.device.domain.bo.IotDeviceBo;
import com.ym.iot.device.domain.vo.IotDeviceHistoryVo;
import com.ym.iot.ownership.domain.IotDeviceOwnership;
import com.ym.iot.ownership.domain.dto.HistoryOwnershipRow;

import org.apache.ibatis.annotations.*;

import java.util.List;

/** 显式租户条件，只读取本租户的设备目录及用于校验的完整归属区间。 */
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface DeviceHistoryMapper
        extends BaseMapperPlus<IotDeviceOwnership, IotDeviceHistoryVo> {
    @Select(
            """
            SELECT device_id, tenant_id, assignment_version, effective_from, effective_to, metadata_snapshot
            FROM iot_device_ownership_history WHERE device_id=#{id} ORDER BY effective_from, assignment_version
            """)
    List<HistoryOwnershipRow> intervals(@Param("id") Long id);

    @Select(
            """
              <script>
              SELECT v.* FROM (
                SELECT h.device_id,
                  CASE WHEN o.tenant_id=#{tenant} THEN d.device_name
                    ELSE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(h.metadata_snapshot,'$.deviceName')),CAST(h.device_id AS CHAR)) END device_name,
                  CASE WHEN o.tenant_id=#{tenant} THEN d.device_code
                    ELSE COALESCE(JSON_UNQUOTE(JSON_EXTRACT(h.metadata_snapshot,'$.deviceCode')),CAST(h.device_id AS CHAR)) END device_code,
                  CASE WHEN o.tenant_id=#{tenant} THEN d.product_id
                    ELSE JSON_UNQUOTE(JSON_EXTRACT(h.metadata_snapshot,'$.productId')) END product_id,
                  CASE WHEN o.tenant_id=#{tenant} THEN p.product_name
                ELSE JSON_UNQUOTE(JSON_EXTRACT(h.metadata_snapshot,'$.productName')) END product_name,
              CASE WHEN o.tenant_id=#{tenant} THEN p.product_key
                ELSE JSON_UNQUOTE(JSON_EXTRACT(h.metadata_snapshot,'$.productKey')) END product_key,
              CASE WHEN o.tenant_id=#{tenant} THEN d.device_category
                    ELSE JSON_UNQUOTE(JSON_EXTRACT(h.metadata_snapshot,'$.deviceCategory')) END device_category,
                  CASE WHEN o.tenant_id=#{tenant} THEN d.online_status ELSE NULL END online_status,
                  CASE WHEN o.tenant_id=#{tenant} THEN d.status ELSE NULL END status,
                  CASE WHEN o.tenant_id=#{tenant} THEN 'CURRENT' ELSE 'TRANSFERRED' END ownership_status
                FROM iot_device_ownership_history h
                JOIN (SELECT device_id,MAX(assignment_version) version FROM iot_device_ownership_history
                      WHERE tenant_id=#{tenant} GROUP BY device_id) own
                  ON h.device_id=own.device_id AND h.assignment_version=own.version
                LEFT JOIN iot_device_ownership o ON o.device_id=h.device_id
                LEFT JOIN iot_device d ON d.device_id=h.device_id
            LEFT JOIN iot_product p ON p.product_id=d.product_id
                WHERE h.tenant_id=#{tenant}
              ) v
              <where>
                <if test="ownershipStatus != 'ALL'">v.ownership_status=#{ownershipStatus}</if>
                <if test="bo != null">
                  <if test="bo.deviceId != null">AND v.device_id=#{bo.deviceId}</if>
                  <if test="bo.productId != null">AND v.product_id=#{bo.productId}</if>
                  <if test="bo.deviceCode != null and !bo.deviceCode.isBlank()">AND v.device_code LIKE CONCAT('%',#{bo.deviceCode},'%')</if>
                  <if test="bo.deviceName != null and !bo.deviceName.isBlank()">AND v.device_name LIKE CONCAT('%',#{bo.deviceName},'%')</if>
                  <if test="bo.deviceCategory != null and !bo.deviceCategory.isBlank()">AND v.device_category=#{bo.deviceCategory}</if>
                  <if test="bo.onlineStatus != null and !bo.onlineStatus.isBlank()">AND v.online_status=#{bo.onlineStatus}</if>
                  <if test="bo.status != null and !bo.status.isBlank()">AND v.status=#{bo.status}</if>
                </if>
              </where>
              ORDER BY v.device_id DESC
              </script>
            """)
    IPage<IotDeviceHistoryVo> visible(
            Page<IotDeviceHistoryVo> page,
            @Param("tenant") String tenant,
            @Param("bo") IotDeviceBo bo,
            @Param("ownershipStatus") String ownershipStatus);
}
