package com.ym.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.domain.PageResult;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.common.mybatis.utils.IdGeneratorUtil;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.system.domain.SysTenantDictData;
import com.ym.system.domain.SysTenantDictType;
import com.ym.system.domain.bo.SysTenantDictDataBo;
import com.ym.system.domain.bo.SysTenantDictTypeBo;
import com.ym.system.domain.vo.SysTenantDictDataVo;
import com.ym.system.domain.vo.SysTenantDictTypeVo;
import com.ym.system.mapper.SysTenantDictDataMapper;
import com.ym.system.mapper.SysTenantDictTypeMapper;
import com.ym.system.service.ISysTenantDictService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 类型全局只读，值由租户在自身数据范围内维护。 */
@Service
@RequiredArgsConstructor
public class SysTenantDictServiceImpl implements ISysTenantDictService {
    private static final String VALUE_PREFIX = "TDV_";
    private final SysTenantDictTypeMapper typeMapper;
    private final SysTenantDictDataMapper dataMapper;

    @Override
    public PageResult<SysTenantDictTypeVo> queryTypePage(SysTenantDictTypeBo bo, PageQuery pageQuery) {
        Page<SysTenantDictTypeVo> page = typeMapper.selectVoPage(pageQuery.build(),
            QueryBuilder.lambda(SysTenantDictType.class)
                .likeIfText(SysTenantDictType::getDictName, bo.getDictName())
                .likeIfText(SysTenantDictType::getDictType, bo.getDictType())
                .orderByAsc(SysTenantDictType::getDictId).build());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SysTenantDictTypeVo> queryTypeOptions() {
        return typeMapper.selectVoList(QueryBuilder.lambda(SysTenantDictType.class)
            .orderByAsc(SysTenantDictType::getDictId).build());
    }

    @Override
    public SysTenantDictTypeVo queryType(Long id) {
        return typeMapper.selectVoById(id);
    }

    @Override
    public PageResult<SysTenantDictDataVo> queryDataPage(SysTenantDictDataBo bo, PageQuery pageQuery) {
        Page<SysTenantDictDataVo> page = dataMapper.selectVoPage(pageQuery.build(),
            QueryBuilder.lambda(SysTenantDictData.class)
                .eqIfText(SysTenantDictData::getDictType, bo.getDictType())
                .likeIfText(SysTenantDictData::getDictLabel, bo.getDictLabel())
                .likeIfText(SysTenantDictData::getDictValue, bo.getDictValue())
                .orderByAsc(SysTenantDictData::getDictType)
                .orderByAsc(SysTenantDictData::getDictSort)
                .orderByAsc(SysTenantDictData::getDictCode).build());
        return PageResult.build(page.getRecords(), page.getTotal());
    }

    @Override
    public List<SysTenantDictDataVo> queryDataByType(String dictType) {
        ensureType(dictType);
        return dataMapper.selectVoList(QueryBuilder.lambda(SysTenantDictData.class)
            .eq(SysTenantDictData::getDictType, dictType.trim())
            .orderByAsc(SysTenantDictData::getDictSort)
            .orderByAsc(SysTenantDictData::getDictCode).build());
    }

    @Override
    public SysTenantDictDataVo queryData(Long id) {
        return dataMapper.selectVoById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertData(SysTenantDictDataBo bo) {
        String dictType = ensureType(bo.getDictType()).getDictType();
        String label = bo.getDictLabel().trim();
        ensureLabelUnique(dictType, label, null);
        SysTenantDictData entity = MapstructUtils.convert(bo, SysTenantDictData.class);
        entity.setDictCode(IdGeneratorUtil.nextLongId());
        entity.setTenantId(TenantHelper.getTenantId());
        entity.setDictType(dictType);
        entity.setDictLabel(label);
        entity.setDictValue(VALUE_PREFIX + Long.toString(IdGeneratorUtil.nextLongId(), Character.MAX_RADIX).toUpperCase(Locale.ROOT));
        entity.setDictSort(bo.getDictSort() == null ? nextSort(dictType) : bo.getDictSort());
        entity.setIsDefault(normalizeDefault(bo.getIsDefault()));
        dataMapper.insert(entity);
        return entity.getDictCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateData(SysTenantDictDataBo bo) {
        SysTenantDictData current = dataMapper.selectById(bo.getDictCode());
        if (current == null) {
            throw new ServiceException("租户字典值不存在或无权访问");
        }
        String dictType = ensureType(bo.getDictType()).getDictType();
        if (!StringUtils.equals(current.getDictType(), dictType)) {
            throw new ServiceException("字典类型不可修改");
        }
        String label = bo.getDictLabel().trim();
        ensureLabelUnique(dictType, label, current.getDictCode());
        current.setDictLabel(label);
        if (bo.getDictSort() != null) {
            current.setDictSort(bo.getDictSort());
        }
        current.setCssClass(bo.getCssClass());
        current.setListClass(bo.getListClass());
        current.setIsDefault(normalizeDefault(bo.getIsDefault()));
        current.setRemark(bo.getRemark());
        dataMapper.updateById(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sortData(List<Long> dictCodes) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            throw new ServiceException("字典排序数据不能为空");
        }
        Set<Long> requestedIds = new HashSet<>(dictCodes);
        if (requestedIds.size() != dictCodes.size()) {
            throw new ServiceException("字典排序数据存在重复项");
        }
        List<SysTenantDictData> selectedRows = dataMapper.selectByIds(dictCodes);
        if (selectedRows.size() != dictCodes.size()) {
            throw new ServiceException("存在不存在或无权访问的字典值，未执行排序");
        }
        String dictType = selectedRows.getFirst().getDictType();
        if (selectedRows.stream().anyMatch(row -> !StringUtils.equals(dictType, row.getDictType()))) {
            throw new ServiceException("只能对同一字典类型的值进行排序");
        }
        List<SysTenantDictData> typeRows = dataMapper.lambda()
            .eq(SysTenantDictData::getDictType, dictType)
            .list();
        Set<Long> typeIds = new HashSet<>(typeRows.stream().map(SysTenantDictData::getDictCode).toList());
        if (!typeIds.equals(requestedIds)) {
            throw new ServiceException("字典值已发生变化，请刷新后重新排序");
        }
        Map<Long, SysTenantDictData> rowMap = new HashMap<>();
        typeRows.forEach(row -> rowMap.put(row.getDictCode(), row));
        for (int index = 0; index < dictCodes.size(); index++) {
            rowMap.get(dictCodes.get(index)).setDictSort(index + 1);
        }
        if (!dataMapper.updateBatchById(typeRows)) {
            throw new ServiceException("字典排序保存失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteData(Collection<Long> ids) {
        List<SysTenantDictData> rows = dataMapper.selectByIds(ids);
        if (rows.size() != ids.size()) {
            throw new ServiceException("存在不存在或无权访问的字典值，未执行删除");
        }
        dataMapper.deleteByIds(ids);
    }

    private SysTenantDictType ensureType(String dictType) {
        String normalized = dictType == null ? null : dictType.trim();
        if (StringUtils.isBlank(normalized)) {
            throw new ServiceException("字典类型不能为空");
        }
        SysTenantDictType type = typeMapper.lambda().eq(SysTenantDictType::getDictType, normalized).one();
        if (type == null) {
            throw new ServiceException("租户字典类型不存在");
        }
        return type;
    }

    private void ensureLabelUnique(String dictType, String label, Long excludedId) {
        if (dataMapper.lambda().eq(SysTenantDictData::getDictType, dictType)
            .eq(SysTenantDictData::getDictLabel, label)
            .neIfPresent(SysTenantDictData::getDictCode, excludedId).exists()) {
            throw new ServiceException("同一字典类型下标签不能重复");
        }
    }

    private int nextSort(String dictType) {
        SysTenantDictData last = dataMapper.lambda().eq(SysTenantDictData::getDictType, dictType)
            .orderByDesc(SysTenantDictData::getDictSort).orderByDesc(SysTenantDictData::getDictCode)
            .last("LIMIT 1").one();
        return last == null || last.getDictSort() == null ? 1 : last.getDictSort() + 1;
    }

    private String normalizeDefault(String value) {
        String normalized = StringUtils.isBlank(value) ? SystemConstants.NO : value.trim();
        if (!SystemConstants.YES.equals(normalized) && !SystemConstants.NO.equals(normalized)) {
            throw new ServiceException("是否默认只能为Y或N");
        }
        return normalized;
    }
}
