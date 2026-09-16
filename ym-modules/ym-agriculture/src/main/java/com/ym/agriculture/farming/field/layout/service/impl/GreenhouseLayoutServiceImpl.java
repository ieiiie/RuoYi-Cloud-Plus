package com.ym.agriculture.farming.field.layout.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.ym.agriculture.farming.field.dao.SfFieldMapper;
import com.ym.agriculture.farming.field.layout.dao.SfGreenhouseLayoutColumnMapper;
import com.ym.agriculture.farming.field.layout.dao.SfGreenhouseLayoutItemMapper;
import com.ym.agriculture.farming.field.layout.dao.SfGreenhouseLayoutMapper;
import com.ym.agriculture.farming.field.layout.model.bo.GreenhouseLayoutDraftBo;
import com.ym.agriculture.farming.field.layout.model.entity.SfGreenhouseLayout;
import com.ym.agriculture.farming.field.layout.model.entity.SfGreenhouseLayoutColumn;
import com.ym.agriculture.farming.field.layout.model.entity.SfGreenhouseLayoutItem;
import com.ym.agriculture.farming.field.layout.model.vo.GreenhouseLayoutVo;
import com.ym.agriculture.farming.field.layout.service.IGreenhouseLayoutService;
import com.ym.agriculture.farming.field.model.bo.SfFieldBo;
import com.ym.agriculture.farming.field.model.constants.FieldType;
import com.ym.agriculture.farming.field.model.vo.SfFieldVo;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.tenant.helper.TenantHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 大棚二维布局聚合服务。
 */
@Service
@RequiredArgsConstructor
public class GreenhouseLayoutServiceImpl implements IGreenhouseLayoutService {

    private static final int DEFAULT_COLUMN_COUNT = 14;
    private static final int MAX_COLUMN_COUNT = 50;
    private static final int MAX_COLUMN_NAME_LENGTH = 32;
    private static final String VERSION_CONFLICT_MESSAGE = "布局已被其他用户更新，请刷新后重试";

    private final SfGreenhouseLayoutMapper layoutMapper;
    private final SfGreenhouseLayoutColumnMapper columnMapper;
    private final SfGreenhouseLayoutItemMapper itemMapper;
    private final SfFieldMapper fieldMapper;

    @Override
    public GreenhouseLayoutVo getLayout() {
        SfFieldBo query = new SfFieldBo();
        query.setFieldType(FieldType.GREENHOUSE);
        return resolveLayout(fieldMapper.selectFieldList(query));
    }

    @Override
    public GreenhouseLayoutVo resolveLayout(List<SfFieldVo> greenhouses) {
        List<SfFieldVo> fields = greenhouses == null ? List.of() : greenhouses;
        SfGreenhouseLayout root = layoutMapper.selectCurrent();
        return root == null ? virtualLayout(fields) : persistedLayout(root, fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLayout(GreenhouseLayoutDraftBo draft, Long currentFieldId) {
        if (draft == null) {
            return;
        }
        long requestedVersion = draft.getVersion() == null ? 0L : draft.getVersion();
        List<ValidatedColumn> validated = validateDraft(draft, currentFieldId);
        SfGreenhouseLayout root = layoutMapper.selectCurrentForUpdate();
        if (root == null) {
            if (requestedVersion != 0L) {
                throw new ServiceException(VERSION_CONFLICT_MESSAGE);
            }
            root = new SfGreenhouseLayout();
            root.setLayoutId(IdWorker.getId());
            root.setTenantId(TenantHelper.getTenantId());
            root.setVersion(1L);
            try {
                layoutMapper.insert(root);
            } catch (DuplicateKeyException exception) {
                throw new ServiceException(VERSION_CONFLICT_MESSAGE);
            }
        } else {
            if (root.getVersion() == null || root.getVersion() != requestedVersion) {
                throw new ServiceException(VERSION_CONFLICT_MESSAGE);
            }
            itemMapper.deleteByLayoutId(root.getLayoutId());
            columnMapper.deleteByLayoutId(root.getLayoutId());
            root.setVersion(root.getVersion() + 1L);
            layoutMapper.updateById(root);
        }
        persistColumns(root.getLayoutId(), validated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFields(Collection<Long> fieldIds) {
        if (fieldIds == null || fieldIds.isEmpty()) {
            return;
        }
        SfGreenhouseLayout root = layoutMapper.selectCurrentForUpdate();
        if (root == null) {
            return;
        }
        if (itemMapper.deleteByFieldIds(fieldIds) > 0) {
            root.setVersion((root.getVersion() == null ? 0L : root.getVersion()) + 1L);
            layoutMapper.updateById(root);
        }
    }

    private void persistColumns(Long layoutId, List<ValidatedColumn> validated) {
        String tenantId = TenantHelper.getTenantId();
        int columnOrder = 1;
        for (ValidatedColumn source : validated) {
            SfGreenhouseLayoutColumn column = new SfGreenhouseLayoutColumn();
            column.setColumnId(IdWorker.getId());
            column.setTenantId(tenantId);
            column.setLayoutId(layoutId);
            column.setColumnName(source.name());
            column.setColumnOrder(columnOrder++);
            columnMapper.insert(column);
            int rowOrder = 1;
            for (Long fieldId : source.fieldIds()) {
                SfGreenhouseLayoutItem item = new SfGreenhouseLayoutItem();
                item.setItemId(IdWorker.getId());
                item.setTenantId(tenantId);
                item.setLayoutId(layoutId);
                item.setColumnId(column.getColumnId());
                item.setFieldId(fieldId);
                item.setRowOrder(rowOrder++);
                itemMapper.insert(item);
            }
        }
    }

    private List<ValidatedColumn> validateDraft(GreenhouseLayoutDraftBo draft, Long currentFieldId) {
        List<GreenhouseLayoutDraftBo.Column> columns = draft.getColumns();
        if (columns == null || columns.isEmpty()) {
            throw new ServiceException("大棚布局至少保留一列");
        }
        if (columns.size() > MAX_COLUMN_COUNT) {
            throw new ServiceException("大棚布局最多支持50列");
        }
        Set<String> names = new HashSet<>();
        Set<Long> allFieldIds = new HashSet<>();
        List<ValidatedColumn> result = new ArrayList<>(columns.size());
        int currentMarkerCount = 0;
        for (GreenhouseLayoutDraftBo.Column source : columns) {
            String name = source == null ? null : StringUtils.trim(source.getColumnName());
            if (StringUtils.isBlank(name)) {
                throw new ServiceException("布局列名称不能为空");
            }
            if (name.length() > MAX_COLUMN_NAME_LENGTH) {
                throw new ServiceException("布局列名称长度不能超过32个字符");
            }
            if (!names.add(name)) {
                throw new ServiceException("布局列名称不能重复");
            }
            List<Long> fieldIds = new ArrayList<>();
            List<GreenhouseLayoutDraftBo.Item> items = source.getItems() == null ? List.of() : source.getItems();
            for (GreenhouseLayoutDraftBo.Item item : items) {
                if (item == null) {
                    throw new ServiceException("布局棚位不能为空");
                }
                Long fieldId;
                if (Boolean.TRUE.equals(item.getCurrentField())) {
                    currentMarkerCount++;
                    if (currentFieldId == null) {
                        throw new ServiceException("当前大棚尚未生成地块ID");
                    }
                    fieldId = currentFieldId;
                } else {
                    fieldId = item.getFieldId();
                }
                if (fieldId == null) {
                    throw new ServiceException("布局棚位必须指定大棚");
                }
                if (!allFieldIds.add(fieldId)) {
                    throw new ServiceException("同一个大棚不能重复出现在布局中");
                }
                fieldIds.add(fieldId);
            }
            result.add(new ValidatedColumn(name, fieldIds));
        }
        if (currentMarkerCount > 1) {
            throw new ServiceException("当前大棚在布局中只能出现一次");
        }
        validateGreenhouseIds(allFieldIds);
        return result;
    }

    private void validateGreenhouseIds(Set<Long> fieldIds) {
        if (fieldIds.isEmpty()) {
            return;
        }
        Map<Long, SfFieldVo> fields = new HashMap<>();
        fieldMapper.selectVoByFieldIds(fieldIds).forEach(field -> fields.put(field.getFieldId(), field));
        for (Long fieldId : fieldIds) {
            SfFieldVo field = fields.get(fieldId);
            if (field == null || !FieldType.GREENHOUSE.equals(FieldType.normalize(field.getFieldType()))) {
                throw new ServiceException("布局只能引用当前租户未删除的大棚");
            }
        }
    }

    private GreenhouseLayoutVo virtualLayout(List<SfFieldVo> fields) {
        GreenhouseLayoutVo result = new GreenhouseLayoutVo();
        result.setVersion(0L);
        result.setInitialized(false);
        result.setUnplacedRows(List.of());
        List<GreenhouseLayoutVo.Column> columns = new ArrayList<>(DEFAULT_COLUMN_COUNT);
        int base = fields.size() / DEFAULT_COLUMN_COUNT;
        int remainder = fields.size() % DEFAULT_COLUMN_COUNT;
        int offset = 0;
        for (int index = 0; index < DEFAULT_COLUMN_COUNT; index++) {
            int size = base + (index < remainder ? 1 : 0);
            GreenhouseLayoutVo.Column column = new GreenhouseLayoutVo.Column();
            column.setColumnName(String.format("%02d棚区", index + 1));
            column.setColumnOrder(index + 1);
            List<GreenhouseLayoutVo.Row> rows = new ArrayList<>(size);
            for (int row = 0; row < size; row++) {
                rows.add(toRow(fields.get(offset++), row + 1));
            }
            column.setRows(rows);
            columns.add(column);
        }
        result.setColumns(columns);
        return result;
    }

    private GreenhouseLayoutVo persistedLayout(SfGreenhouseLayout root, List<SfFieldVo> fields) {
        Map<Long, SfFieldVo> fieldById = new LinkedHashMap<>();
        fields.forEach(field -> fieldById.put(field.getFieldId(), field));
        Map<Long, List<SfGreenhouseLayoutItem>> itemsByColumn = new HashMap<>();
        for (SfGreenhouseLayoutItem item : itemMapper.selectByLayoutId(root.getLayoutId())) {
            itemsByColumn.computeIfAbsent(item.getColumnId(), ignored -> new ArrayList<>()).add(item);
        }
        Set<Long> placed = new HashSet<>();
        List<GreenhouseLayoutVo.Column> columns = new ArrayList<>();
        for (SfGreenhouseLayoutColumn source : columnMapper.selectByLayoutId(root.getLayoutId())) {
            GreenhouseLayoutVo.Column column = new GreenhouseLayoutVo.Column();
            column.setColumnName(source.getColumnName());
            column.setColumnOrder(source.getColumnOrder());
            List<GreenhouseLayoutVo.Row> rows = new ArrayList<>();
            for (SfGreenhouseLayoutItem item : itemsByColumn.getOrDefault(source.getColumnId(), List.of())) {
                SfFieldVo field = fieldById.get(item.getFieldId());
                if (field != null && placed.add(field.getFieldId())) {
                    rows.add(toRow(field, item.getRowOrder()));
                }
            }
            column.setRows(rows);
            columns.add(column);
        }
        if (columns.isEmpty()) {
            GreenhouseLayoutVo.Column column = new GreenhouseLayoutVo.Column();
            column.setColumnName("01棚区");
            column.setColumnOrder(1);
            column.setRows(new ArrayList<>());
            columns.add(column);
        }
        for (SfFieldVo field : fields) {
            if (!placed.contains(field.getFieldId())) {
                GreenhouseLayoutVo.Column target = columns.stream()
                    .min((left, right) -> {
                        int sizeCompare = Integer.compare(left.getRows().size(), right.getRows().size());
                        return sizeCompare != 0 ? sizeCompare
                            : Integer.compare(left.getColumnOrder(), right.getColumnOrder());
                    })
                    .orElseThrow();
                target.getRows().add(toRow(field, target.getRows().size() + 1));
            }
        }
        GreenhouseLayoutVo result = new GreenhouseLayoutVo();
        result.setVersion(root.getVersion() == null ? 0L : root.getVersion());
        result.setInitialized(true);
        result.setColumns(columns);
        result.setUnplacedRows(List.of());
        return result;
    }

    private GreenhouseLayoutVo.Row toRow(SfFieldVo field, int rowOrder) {
        GreenhouseLayoutVo.Row row = new GreenhouseLayoutVo.Row();
        row.setFieldId(field.getFieldId());
        row.setFieldCode(field.getFieldCode());
        row.setFieldName(field.getFieldName());
        row.setStatus(field.getStatus());
        row.setSortOrder(field.getSortOrder());
        row.setAreaMu(field.getAreaMu());
        row.setRowOrder(rowOrder);
        return row;
    }

    private record ValidatedColumn(String name, List<Long> fieldIds) {
    }
}
