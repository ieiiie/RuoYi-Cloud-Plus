package com.ym.system.dubbo;

import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboService;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.system.api.RemoteDictService;
import com.ym.system.api.domain.vo.RemoteDictDataVo;
import com.ym.system.api.domain.vo.RemoteDictTypeVo;
import com.ym.system.domain.vo.SysDictDataVo;
import com.ym.system.domain.vo.SysDictTypeVo;
import com.ym.system.service.ISysDictTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
@DubboService
public class RemoteDictServiceImpl implements RemoteDictService {

    private final ISysDictTypeService sysDictTypeService;

    /**
     * remote根据字典类型查询字典
     *
     * @param dictType 字典类型
     * @return RemoteDictTypeVo
     * @see com.ym.system.domain.convert.SysDictTypeVoConvert
     */
    @Override
    public RemoteDictTypeVo selectDictTypeByType(String dictType) {
        SysDictTypeVo vo = sysDictTypeService.selectDictTypeByType(dictType);
        return MapstructUtils.convert(vo, RemoteDictTypeVo.class);
    }

    /**
     * remote根据字典类型查询字典数据
     *
     * @param dictType 字典类型
     * @return 字典数据集合信息
     * @see com.ym.system.domain.convert.SysDictDataVoConvert
     */
    @Override
    public List<RemoteDictDataVo> selectDictDataByType(String dictType) {
        List<SysDictDataVo> list = sysDictTypeService.selectDictDataByType(dictType);
        return MapstructUtils.convert(list, RemoteDictDataVo.class);
    }

}
