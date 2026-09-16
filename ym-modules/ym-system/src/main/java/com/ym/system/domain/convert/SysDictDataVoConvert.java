package com.ym.system.domain.convert;

import io.github.linpeilie.BaseMapper;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import com.ym.system.domain.vo.SysDictDataVo;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * 字典数据转换器
 *
 * @author zhujie
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface SysDictDataVoConvert extends BaseMapper<SysDictDataVo, RemoteDictDataVo> {

}
