package com.ym.agriculture.farming.farmwork.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.bo.SfFarmWorkDictBo;
import com.ym.agriculture.farming.farmwork.model.bo.SfFarmWorkDictSortBo;
import com.ym.agriculture.farming.farmwork.model.constants.FarmWorkNodeType;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictTreeVo;
import com.ym.agriculture.farming.farmwork.model.vo.SfFarmWorkDictVo;
import com.ym.agriculture.farming.farmwork.service.ISfFarmWorkDictService;
import com.ym.common.json.utils.JsonUtils;
import tools.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * stask 农事字典服务实现。
 *
 * @author ym-cloud
 */
@RequiredArgsConstructor
@Service
public class SfFarmWorkDictServiceImpl implements ISfFarmWorkDictService {

    private static final long ROOT_PARENT_ID = 0L;
    private static final int DEFAULT_MIN_WORKERS = 1;
    private static final int DEFAULT_MAX_WORKERS = 30;

    private final SfFarmWorkDictMapper dictMapper;

    @Override
    public List<SfFarmWorkDictTreeVo> tree() {
        String tenantId = requireTenantId();
        List<SfFarmWorkDict> rows = dictMapper.selectNormalList(tenantId);
        if (rows.isEmpty()) {
            return List.of();
        }
        Map<Long, List<SfFarmWorkDict>> childrenByParent = rows.stream()
            .filter(row -> !Objects.equals(row.getParentId(), ROOT_PARENT_ID))
            .collect(Collectors.groupingBy(SfFarmWorkDict::getParentId));

        List<SfFarmWorkDictTreeVo> tree = new ArrayList<>();
        rows.stream()
            .filter(row -> Objects.equals(row.getParentId(), ROOT_PARENT_ID))
            .sorted(Comparator.comparing(SfFarmWorkDict::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(SfFarmWorkDict::getDictId))
            .forEach(category -> {
                SfFarmWorkDictTreeVo node = toTreeVo(category);
                List<SfFarmWorkDictTreeVo> children = childrenByParent
                    .getOrDefault(category.getDictId(), List.of())
                    .stream()
                    .sorted(Comparator.comparing(SfFarmWorkDict::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SfFarmWorkDict::getDictId))
                    .map(this::toTreeVo)
                    .toList();
                node.setChildren(new ArrayList<>(children));
                tree.add(node);
            });
        return tree;
    }

    @Override
    public List<SfFarmWorkDictTreeVo> miniappTree() {
        List<SfFarmWorkDictTreeVo> tree = tree();
        if (tree.isEmpty()) {
            return List.of();
        }
        List<SfFarmWorkDictTreeVo> result = new ArrayList<>();
        for (SfFarmWorkDictTreeVo category : tree) {
            List<SfFarmWorkDictTreeVo> enabledItems = category.getChildren().stream()
                .filter(item -> SystemConstants.NORMAL.equals(item.getStatus()))
                .toList();
            if (enabledItems.isEmpty()) {
                continue;
            }
            SfFarmWorkDictTreeVo node = BeanUtil.copyProperties(category, SfFarmWorkDictTreeVo.class);
            node.setChildren(new ArrayList<>(enabledItems));
            result.add(node);
        }
        return result;
    }

    @Override
    public List<SfFarmWorkDictVo> mobileCategories() {
        String tenantId = requireTenantId();
        List<Long> categoryIds = dictMapper.selectMobileEnabledItems(tenantId).stream()
            .map(SfFarmWorkDict::getParentId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return dictMapper.selectMobileCategories(tenantId, categoryIds).stream()
            .map(this::toVo)
            .toList();
    }

    @Override
    public List<SfFarmWorkDictVo> mobileItems(Long categoryId) {
        String tenantId = requireTenantId();
        SfFarmWorkDict category = dictMapper.selectNormalById(tenantId, categoryId);
        if (category == null || !FarmWorkNodeType.isCategory(category.getNodeType())) {
            throw new ServiceException("农事分类不存在或无权访问");
        }
        return dictMapper.selectMobileEnabledItemsByCategoryId(tenantId, categoryId).stream()
            .map(this::toVo)
            .toList();
    }

    @Override
    public SfFarmWorkDictVo get(Long dictId) {
        SfFarmWorkDict row = requireNode(dictId, "农事字典不存在");
        return toVo(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(SfFarmWorkDictBo bo) {
        String tenantId = requireTenantId();
        String nodeType = normalizeNodeType(bo.getNodeType());
        Long parentId = normalizeParentId(bo.getParentId());
        validateParent(tenantId, nodeType, parentId, null);
        String dictCode = normalizeCode(bo.getDictCode());
        if (dictMapper.existsTenantCode(tenantId, dictCode, null)) {
            throw new ServiceException("编码已存在");
        }
        String dictName = normalizeName(bo.getDictName());
        validateNameUnique(tenantId, nodeType, parentId, dictName, null);
        validateWorkers(nodeType, bo.getMinWorkers(), bo.getMaxWorkers());

        LocalDateTime now = LocalDateTime.now();
        Long userId = LoginHelper.getUserId();
        SfFarmWorkDict row = new SfFarmWorkDict();
        row.setDictId(IdWorker.getId());
        row.setTenantId(tenantId);
        row.setParentId(parentId);
        row.setNodeType(nodeType);
        row.setDictName(dictName);
        row.setDictCode(dictCode);
        fillItemFields(row, bo);
        row.setSortOrder(bo.getSortOrder() != null ? bo.getSortOrder() : dictMapper.selectMaxSortOrder(tenantId, parentId) + 1);
        row.setDelFlag(SystemConstants.NORMAL);
        row.setRemark(bo.getRemark());
        row.setCreateBy(userId);
        row.setCreateTime(now);
        row.setUpdateBy(userId);
        row.setUpdateTime(now);
        return dictMapper.insert(row) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(SfFarmWorkDictBo bo) {
        String tenantId = requireTenantId();
        SfFarmWorkDict old = requireNode(bo.getDictId(), "农事字典不存在");
        String nodeType = normalizeNodeType(bo.getNodeType());
        if (!nodeType.equals(old.getNodeType())) {
            throw new ServiceException("节点类型不允许修改");
        }
        Long parentId = normalizeParentId(bo.getParentId());
        if (FarmWorkNodeType.isItem(nodeType) && !parentId.equals(old.getParentId())) {
            throw new ServiceException("项目不支持跨分类移动");
        }
        validateParent(tenantId, nodeType, parentId, old.getDictId());
        String dictCode = normalizeCode(bo.getDictCode());
        if (dictMapper.existsTenantCode(tenantId, dictCode, old.getDictId())) {
            throw new ServiceException("编码已存在");
        }
        String dictName = normalizeName(bo.getDictName());
        validateNameUnique(tenantId, nodeType, parentId, dictName, old.getDictId());
        validateWorkers(nodeType, bo.getMinWorkers(), bo.getMaxWorkers());

        old.setParentId(parentId);
        old.setDictName(dictName);
        old.setDictCode(dictCode);
        fillItemFields(old, bo);
        if (bo.getSortOrder() != null) {
            old.setSortOrder(bo.getSortOrder());
        }
        old.setRemark(bo.getRemark());
        old.setUpdateBy(LoginHelper.getUserId());
        old.setUpdateTime(LocalDateTime.now());
        boolean updated = dictMapper.updateById(old) > 0;
        if (updated) {
            if (FarmWorkNodeType.isItem(nodeType) && bo.getCustomFormTemplate() != null && old.getCustomFormTemplateJson() == null) {
                dictMapper.clearCustomFormTemplateJson(tenantId, old.getDictId());
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long dictId) {
        String tenantId = requireTenantId();
        SfFarmWorkDict row = requireNode(dictId, "农事字典不存在");
        if (FarmWorkNodeType.isCategory(row.getNodeType())) {
            long count = dictMapper.countNormalChildren(tenantId, dictId);
            if (count > 0) {
                throw new ServiceException("该分类下存在 " + count + " 个项目，请先删除所有项目");
            }
        }
        return dictMapper.deleteById(dictId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean sort(List<SfFarmWorkDictSortBo> sortList) {
        String tenantId = requireTenantId();
        List<Long> ids = sortList.stream().map(SfFarmWorkDictSortBo::getDictId).distinct().toList();
        List<SfFarmWorkDict> rows = dictMapper.selectNormalByIds(tenantId, ids);
        if (rows.size() != ids.size()) {
            throw new ServiceException("排序节点不存在或无权访问");
        }
        Map<Long, SfFarmWorkDict> rowMap = rows.stream()
            .collect(Collectors.toMap(SfFarmWorkDict::getDictId, Function.identity()));
        Long parentId = null;
        String nodeType = null;
        for (SfFarmWorkDictSortBo item : sortList) {
            SfFarmWorkDict row = rowMap.get(item.getDictId());
            if (parentId == null) {
                parentId = row.getParentId();
                nodeType = row.getNodeType();
            } else if (!parentId.equals(row.getParentId()) || !nodeType.equals(row.getNodeType())) {
                throw new ServiceException("只能对同一分类下的同类型节点排序");
            }
        }
        List<SfFarmWorkDict> updates = sortList.stream()
            .map(item -> buildSortUpdate(tenantId, item))
            .toList();
        dictMapper.updateBatchById(updates);
        return true;
    }

    @Override
    public boolean checkCodeUnique(String dictCode, Long excludeDictId) {
        String tenantId = requireTenantId();
        return !dictMapper.existsTenantCode(tenantId, normalizeCode(dictCode), excludeDictId);
    }

    @Override
    public boolean checkNameUnique(String dictName, String nodeType, Long parentId, Long excludeDictId) {
        String tenantId = requireTenantId();
        String normalizedNodeType = normalizeNodeType(nodeType);
        String normalizedName = normalizeName(dictName);
        if (FarmWorkNodeType.isItem(normalizedNodeType) && parentId != null) {
            return !dictMapper.existsSiblingName(tenantId, normalizeParentId(parentId), normalizedName,
                normalizedNodeType, excludeDictId);
        }
        return !dictMapper.existsTenantName(tenantId, normalizedName, normalizedNodeType, excludeDictId);
    }

    private SfFarmWorkDict requireNode(Long dictId, String message) {
        String tenantId = requireTenantId();
        SfFarmWorkDict row = dictMapper.selectNormalById(tenantId, dictId);
        if (row == null) {
            throw new ServiceException(message);
        }
        return row;
    }

    private SfFarmWorkDictTreeVo toTreeVo(SfFarmWorkDict row) {
        SfFarmWorkDictVo vo = MapstructUtils.convert(row, SfFarmWorkDictVo.class);
        fillCustomFormTemplate(row, vo);
        return BeanUtil.copyProperties(vo, SfFarmWorkDictTreeVo.class);
    }

    private SfFarmWorkDictVo toVo(SfFarmWorkDict row) {
        SfFarmWorkDictVo vo = BeanUtil.copyProperties(row, SfFarmWorkDictVo.class);
        fillCustomFormTemplate(row, vo);
        return vo;
    }

    private void fillCustomFormTemplate(SfFarmWorkDict row, SfFarmWorkDictVo vo) {
        if (FarmWorkNodeType.isItem(row.getNodeType())) {
            vo.setCustomFormTemplate(JsonUtils.parseObject(row.getCustomFormTemplateJson(),
                new TypeReference<Map<String, Object>>() {
                }));
        } else {
            vo.setCustomFormTemplate(null);
        }
    }

    private static String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StringUtils.isBlank(tenantId)) {
            throw new ServiceException("租户上下文缺失");
        }
        return tenantId;
    }

    private static Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private static SfFarmWorkDict buildSortUpdate(String tenantId, SfFarmWorkDictSortBo item) {
        SfFarmWorkDict row = new SfFarmWorkDict();
        row.setDictId(item.getDictId());
        row.setTenantId(tenantId);
        row.setSortOrder(item.getSortOrder());
        return row;
    }

    private static String normalizeNodeType(String nodeType) {
        if (StringUtils.isBlank(nodeType)) {
            throw new ServiceException("节点类型不能为空");
        }
        return nodeType.trim();
    }

    private static String normalizeCode(String dictCode) {
        if (StringUtils.isBlank(dictCode)) {
            throw new ServiceException("编码不能为空");
        }
        return dictCode.trim();
    }

    private static String normalizeName(String dictName) {
        if (StringUtils.isBlank(dictName)) {
            throw new ServiceException("名称不能为空");
        }
        return dictName.trim();
    }

    private void validateNameUnique(String tenantId, String nodeType, Long parentId, String dictName, Long excludeDictId) {
        boolean exists = FarmWorkNodeType.isCategory(nodeType)
            ? dictMapper.existsTenantName(tenantId, dictName, nodeType, excludeDictId)
            : dictMapper.existsSiblingName(tenantId, parentId, dictName, nodeType, excludeDictId);
        if (exists) {
            throw new ServiceException(FarmWorkNodeType.isCategory(nodeType) ? "分类名称已存在" : "项目名称已存在");
        }
    }

    private void validateParent(String tenantId, String nodeType, Long parentId, Long selfId) {
        if (FarmWorkNodeType.isCategory(nodeType)) {
            if (!Objects.equals(parentId, ROOT_PARENT_ID)) {
                throw new ServiceException("分类的父节点必须为0");
            }
            return;
        }
        if (!FarmWorkNodeType.isItem(nodeType)) {
            throw new ServiceException("节点类型只能为CATEGORY或ITEM");
        }
        if (Objects.equals(parentId, ROOT_PARENT_ID)) {
            throw new ServiceException("项目必须归属于一个分类");
        }
        if (Objects.equals(parentId, selfId)) {
            throw new ServiceException("项目不能归属于自身");
        }
        SfFarmWorkDict parent = dictMapper.selectNormalById(tenantId, parentId);
        if (parent == null || !FarmWorkNodeType.isCategory(parent.getNodeType())) {
            throw new ServiceException("所属分类不存在");
        }
    }

    private static void validateWorkers(String nodeType, Integer minWorkers, Integer maxWorkers) {
        if (!FarmWorkNodeType.isItem(nodeType)) {
            return;
        }
        int min = minWorkers == null ? DEFAULT_MIN_WORKERS : minWorkers;
        int max = maxWorkers == null ? DEFAULT_MAX_WORKERS : maxWorkers;
        if (min < 0) {
            throw new ServiceException("最少工人数不能小于0");
        }
        if (max < 0) {
            throw new ServiceException("最多工人数不能小于0");
        }
        if (min > max) {
            throw new ServiceException("最少工人数不能大于最多工人数");
        }
    }

    private static void fillItemFields(SfFarmWorkDict row, SfFarmWorkDictBo bo) {
        if (FarmWorkNodeType.isCategory(row.getNodeType())) {
            row.setMinWorkers(null);
            row.setMaxWorkers(null);
            row.setRequiresMaterial(false);
            row.setStatus(null);
            row.setCustomFormTemplateJson(null);
            return;
        }
        row.setMinWorkers(bo.getMinWorkers() == null ? DEFAULT_MIN_WORKERS : bo.getMinWorkers());
        row.setMaxWorkers(bo.getMaxWorkers() == null ? DEFAULT_MAX_WORKERS : bo.getMaxWorkers());
        row.setRequiresMaterial(Boolean.TRUE.equals(bo.getRequiresMaterial()));
        row.setStatus(StringUtils.isBlank(bo.getStatus()) ? SystemConstants.NORMAL : bo.getStatus().trim());
        if (bo.getCustomFormTemplate() != null) {
            row.setCustomFormTemplateJson(JsonUtils.toJsonString(bo.getCustomFormTemplate()));
        }
    }
}
