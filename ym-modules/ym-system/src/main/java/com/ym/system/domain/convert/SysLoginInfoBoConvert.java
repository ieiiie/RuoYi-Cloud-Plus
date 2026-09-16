package com.ym.system.domain.convert;

import io.github.linpeilie.BaseMapper;
import com.ym.system.api.domain.bo.RemoteLoginInfoBo;
import com.ym.system.domain.bo.SysLoginInfoBo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 登录日志转换器
 *
 * @author zhujie
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SysLoginInfoBoConvert extends BaseMapper<RemoteLoginInfoBo, SysLoginInfoBo> {

}
