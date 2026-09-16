package com.ym.system.domain.convert;

import io.github.linpeilie.BaseMapper;
import com.ym.system.api.domain.vo.RemoteUserVo;
import com.ym.system.domain.vo.SysUserVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 租户转换器
 *
 * @author zhujie
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SysUserVoConvert extends BaseMapper<SysUserVo, RemoteUserVo> {

}
