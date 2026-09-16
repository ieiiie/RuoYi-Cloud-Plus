package com.ym.agriculture.farming.field.support;

import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.vo.RemoteUserVo;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.List;

/** 地块与农事业务访问平台用户的 Dubbo 边界。 */
@Component
public class SfFieldMasterUserAccessor {

    @DubboReference
    private RemoteUserService remoteUserService;

    public RemoteUserVo selectUserById(Long userId) {
        if (userId == null) {
            return null;
        }
        return remoteUserService.selectListByIds(List.of(userId)).stream().findFirst().orElse(null);
    }

    public List<RemoteUserVo> selectUserByIds(List<Long> userIds, Long deptId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        List<RemoteUserVo> rows = remoteUserService.selectListByIds(userIds);
        if (rows == null) {
            return List.of();
        }
        if (deptId == null) {
            return rows;
        }
        return rows.stream().filter(user -> deptId.equals(user.getDeptId())).toList();
    }
}
