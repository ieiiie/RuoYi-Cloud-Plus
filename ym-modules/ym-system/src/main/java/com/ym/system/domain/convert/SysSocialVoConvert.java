package com.ym.system.domain.convert;

import io.github.linpeilie.BaseMapper;
import com.ym.system.api.domain.vo.RemoteSocialVo;
import com.ym.system.domain.vo.SysSocialVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 社交数据转换器
 *
 * @author Michelle.Chung
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SysSocialVoConvert extends BaseMapper<SysSocialVo, RemoteSocialVo> {
}
