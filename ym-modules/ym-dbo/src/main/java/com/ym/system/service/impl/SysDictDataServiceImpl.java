package com.ym.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.constant.CacheNames;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.redis.utils.CacheUtils;
import com.ym.system.domain.SysDictData;
import com.ym.system.domain.bo.SysDictDataBo;
import com.ym.system.domain.vo.SysDictDataVo;
import com.ym.system.mapper.SysDictDataMapper;
import com.ym.system.service.ISysDictDataService;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * 字典 业务层处理
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@Service
public class SysDictDataServiceImpl implements ISysDictDataService {

    private final SysDictDataMapper dictDataMapper;

    /**
     * 分页查询字典数据列表
     *
     * @param dictData  查询条件
     * @param pageQuery 分页参数
     * @return 字典数据分页列表
     */
    @Override
    public PageResult<SysDictDataVo> selectPageDictDataList(SysDictDataBo dictData, PageQuery pageQuery) {
        LambdaQueryWrapper<SysDictData> lqw = buildQueryWrapper(dictData);
        Page<SysDictDataVo> page = dictDataMapper.selectVoPage(pageQuery.build(), lqw);
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    /**
     * 根据条件分页查询字典数据
     *
     * @param dictData 字典数据信息
     * @return 字典数据集合信息
     */
    @Override
    public List<SysDictDataVo> selectDictDataList(SysDictDataBo dictData) {
        LambdaQueryWrapper<SysDictData> lqw = buildQueryWrapper(dictData);
        return dictDataMapper.selectVoList(lqw);
    }

    /**
     * 构造字典数据列表查询条件。
     *
     * @param bo 字典数据筛选条件
     * @return 包含排序号、标签和字典类型条件的查询包装器
     */
    private LambdaQueryWrapper<SysDictData> buildQueryWrapper(SysDictDataBo bo) {
        return QueryBuilder.lambda(SysDictData.class)
            .eqIfPresent(SysDictData::getDictSort, bo.getDictSort())
            .likeIfText(SysDictData::getDictLabel, bo.getDictLabel())
            .eqIfText(SysDictData::getDictType, bo.getDictType())
            .orderByAsc(SysDictData::getDictSort, SysDictData::getDictCode)
            .build();
    }

    /**
     * 根据字典类型和字典键值查询字典数据信息
     *
     * @param dictType  字典类型
     * @param dictValue 字典键值
     * @return 字典标签
     */
    @Override
    public String selectDictLabel(String dictType, String dictValue) {
        return dictDataMapper.lambda()
            .select(SysDictData::getDictLabel)
            .eq(SysDictData::getDictType, dictType)
            .eq(SysDictData::getDictValue, dictValue)
            .one()
            .getDictLabel();
    }

    /**
     * 根据字典数据ID查询信息
     *
     * @param dictCode 字典数据ID
     * @return 字典数据
     */
    @Override
    public SysDictDataVo selectDictDataById(Long dictCode) {
        return dictDataMapper.selectVoById(dictCode);
    }

    /**
     * 批量删除字典数据信息
     *
     * @param dictCodes 需要删除的字典数据ID
     */
    @Override
    public void deleteDictDataByIds(Collection<Long> dictCodes) {
        List<SysDictData> list = dictDataMapper.selectByIds(dictCodes);
        dictDataMapper.deleteByIds(dictCodes);
        list.forEach(x -> CacheUtils.evict(CacheNames.SYS_DICT, x.getDictType()));
    }

    /**
     * 新增保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> insertDictData(SysDictDataBo bo) {
        SysDictData data = MapstructUtils.convert(bo, SysDictData.class);
        int row = dictDataMapper.insert(data);
        if (row > 0) {
            return dictDataMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 修改保存字典数据信息
     *
     * @param bo 字典数据信息
     * @return 结果
     */
    @CachePut(cacheNames = CacheNames.SYS_DICT, key = "#bo.dictType")
    @Override
    public List<SysDictDataVo> updateDictData(SysDictDataBo bo) {
        SysDictData data = MapstructUtils.convert(bo, SysDictData.class);
        int row = dictDataMapper.updateById(data);
        if (row > 0) {
            return dictDataMapper.selectDictDataByType(data.getDictType());
        }
        throw new ServiceException("操作失败");
    }

    /**
     * 校验字典键值是否唯一
     *
     * @param dict 字典数据
     * @return 结果
     */
    @Override
    public boolean checkDictDataUnique(SysDictDataBo dict) {
        boolean exist = dictDataMapper.lambda()
            .eq(SysDictData::getDictType, dict.getDictType())
            .eq(SysDictData::getDictValue, dict.getDictValue())
            .neIfPresent(SysDictData::getDictCode, dict.getDictCode())
            .exists();
        return !exist;
    }

}
