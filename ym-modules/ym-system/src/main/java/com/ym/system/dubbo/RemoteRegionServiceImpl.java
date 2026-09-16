package com.ym.system.dubbo;

import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.api.RemoteRegionService;
import com.ym.system.domain.SysRegion;
import com.ym.system.mapper.SysRegionMapper;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

/** 全局行政区划远程查询实现。 */
@Service
@DubboService
@RequiredArgsConstructor
public class RemoteRegionServiceImpl implements RemoteRegionService {

    private final SysRegionMapper regionMapper;

    @Override
    public String getRegionName(String adcode) {
        if (StringUtils.isBlank(adcode)) {
            return null;
        }
        return TenantHelper.ignore(() -> {
            SysRegion region = regionMapper.lambda()
                .eq(SysRegion::getAdcode, adcode.trim())
                .last("LIMIT 1")
                .one();
            return region == null ? null : region.getRegionName();
        });
    }
}
