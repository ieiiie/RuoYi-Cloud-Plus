package com.ym.agriculture.farmtask.sop.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.HttpStatus;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.shared.i18n.StaskMessageKeys;
import com.ym.agriculture.shared.i18n.StaskMessageResolver;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskDispatchMapper;
import com.ym.agriculture.farmtask.workorder.support.SfStaskEmployeeAccessor;
import com.ym.agriculture.farming.farmwork.dao.SfFarmWorkDictMapper;
import com.ym.agriculture.farming.farmwork.model.entity.SfFarmWorkDict;
import com.ym.agriculture.farmtask.sop.dao.SfStaskSopMapper;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopBo;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopQueryBo;
import com.ym.agriculture.farmtask.sop.model.constants.StaskSopCropScope;
import com.ym.agriculture.farmtask.sop.model.constants.StaskSopLanguage;
import com.ym.agriculture.farmtask.sop.model.entity.SfStaskSop;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopResolveVo;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopVo;
import com.ym.agriculture.farmtask.sop.service.ISfStaskSopService;
import com.ym.agriculture.farmtask.sop.support.StaskSopContentValidator;
import com.ym.agriculture.farmtask.workorder.dao.SfStaskWorkOrderMapper;
import com.ym.agriculture.farmtask.workorder.model.constants.StaskOrderStatus;
import com.ym.agriculture.farmtask.workorder.model.entity.SfStaskWorkOrder;
import com.ym.agriculture.farmtask.workorder.service.ISfStaskWorkOrderService;
import com.ym.agriculture.farming.batch.service.ISfPlantingBatchService;
import com.ym.agriculture.farming.batch.model.constants.PlantingBatchStatus;
import com.ym.agriculture.farming.batch.model.vo.SfPlantingBatchVo;
import com.ym.agriculture.farming.crop.dao.SfCropSpeciesMapper;
import com.ym.agriculture.farming.crop.model.entity.SfCropSpecies;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** 农事 SOP 服务实现。 */
@RequiredArgsConstructor
@Service
public class SfStaskSopServiceImpl implements ISfStaskSopService {

    private static final long ALL_CROP_SPECIES_ID = 0L;
    private static final Set<String> ELIGIBLE_STATUSES = Set.of(
        StaskOrderStatus.PENDING_LEADER_ACCEPT,
        StaskOrderStatus.ASSIGN_COMPLETE,
        StaskOrderStatus.LEADER_ARRIVED,
        StaskOrderStatus.PENDING_ACCEPTANCE,
        StaskOrderStatus.ACCEPTANCE_PASSED,
        StaskOrderStatus.ACCEPTANCE_REJECTED
    );

    private final SfStaskSopMapper sopMapper;
    private final SfFarmWorkDictMapper farmWorkDictMapper;
    private final SfCropSpeciesMapper cropSpeciesMapper;
    private final SfStaskWorkOrderMapper workOrderMapper;
    private final SfStaskDispatchMapper dispatchMapper;
    private final ISfStaskWorkOrderService workOrderService;
    private final SfStaskEmployeeAccessor employeeAccessor;
    private final ISfPlantingBatchService plantingBatchService;
    private final StaskSopContentValidator contentValidator;
    private final StaskMessageResolver messages;

    @Override
    public PageResult<SfStaskSopVo> page(SfStaskSopQueryBo bo, PageQuery pageQuery) {
        String tenantId = requireTenantId();
        Page<SfStaskSop> entityPage = sopMapper.selectPage(pageQuery.build(), Wrappers.<SfStaskSop>lambdaQuery()
            .eq(SfStaskSop::getTenantId, tenantId)
            .eq(bo.getWorkItemId() != null, SfStaskSop::getWorkItemId, bo.getWorkItemId())
            .eq(StrUtil.isNotBlank(bo.getCropScope()), SfStaskSop::getCropScope, bo.getCropScope())
            .eq(StrUtil.isNotBlank(bo.getLanguage()), SfStaskSop::getLanguage, bo.getLanguage())
            .eq(bo.getCropSpeciesId() != null, SfStaskSop::getCropSpeciesId, bo.getCropSpeciesId())
            .orderByDesc(SfStaskSop::getUpdateTime));
        Page<SfStaskSopVo> result = new Page<>(entityPage.getCurrent(), entityPage.getSize(), entityPage.getTotal());
        result.setRecords(toVos(entityPage.getRecords(), false));
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(result);
    }

    @Override
    public SfStaskSopVo getById(Long sopId) {
        return toVo(requireSop(sopId), true);
    }

    @Override
    public Long add(SfStaskSopBo bo) {
        String tenantId = requireTenantId();
        SfStaskSop entity = BeanUtil.toBean(bo, SfStaskSop.class);
        normalizeAndValidate(entity, bo, tenantId);
        try {
            sopMapper.insert(entity);
            return entity.getSopId();
        } catch (DuplicateKeyException exception) {
            throw duplicateSopException();
        }
    }

    @Override
    public void update(Long sopId, SfStaskSopBo bo) {
        SfStaskSop current = requireSop(sopId);
        SfStaskSop entity = BeanUtil.toBean(bo, SfStaskSop.class);
        entity.setSopId(current.getSopId());
        entity.setTenantId(current.getTenantId());
        normalizeAndValidate(entity, bo, current.getTenantId());
        try {
            sopMapper.updateById(entity);
        } catch (DuplicateKeyException exception) {
            throw duplicateSopException();
        }
    }

    @Override
    public void remove(Long sopId) {
        String tenantId = requireTenantId();
        int count = sopMapper.delete(Wrappers.<SfStaskSop>lambdaQuery()
            .eq(SfStaskSop::getSopId, sopId)
            .eq(SfStaskSop::getTenantId, tenantId));
        if (count == 0) {
            throw new ServiceException("农事SOP不存在或无权操作");
        }
    }

    @Override
    public SfStaskSopVo copyDraft(Long sopId) {
        SfStaskSopVo draft = toVo(requireSop(sopId), true);
        draft.setSopId(null);
        draft.setCreateTime(null);
        draft.setUpdateTime(null);
        return draft;
    }

    @Override
    public SfStaskSopResolveVo resolveForMiniapp(Long orderId) {
        String tenantId = requireTenantId();
        SfStaskWorkOrder order = workOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(tenantId, order.getTenantId())) {
            throw new ServiceException("工单不存在或无权访问", HttpStatus.FORBIDDEN);
        }
        assertOrderVisible(order);

        SfStaskSopResolveVo result = new SfStaskSopResolveVo();
        String requestedLanguage = resolveRequestedLanguage();
        result.setEligible(ELIGIBLE_STATUSES.contains(order.getStatus()));
        result.setMatched(false);
        result.setRequestedLanguage(requestedLanguage);
        result.setLanguageFallback(false);
        if (!Boolean.TRUE.equals(result.getEligible())) {
            return result;
        }

        Long cropSpeciesId = resolveActiveCropSpeciesId(order.getGreenhouseId());
        SfStaskSop matched = selectMatchedSop(tenantId, order.getWorkItemId(), cropSpeciesId, requestedLanguage);
        if (matched == null) {
            return result;
        }

        result.setMatched(true);
        result.setContentLanguage(matched.getLanguage());
        result.setLanguageFallback(!Objects.equals(requestedLanguage, matched.getLanguage()));
        result.setContentBlocks(contentValidator.parseForMiniapp(matched.getContentBlocks()));
        return result;
    }

    private void normalizeAndValidate(SfStaskSop entity, SfStaskSopBo bo, String tenantId) {
        if (!StaskSopLanguage.ALL.contains(bo.getLanguage())) {
            throw new ServiceException("language 仅支持 zh-CN 或 ug-CN");
        }
        if (!StaskSopCropScope.SUPPORTED.contains(bo.getCropScope())) {
            throw new ServiceException("cropScope 仅支持 ALL 或 SPECIFIC");
        }
        assertEnabledWorkItem(bo.getWorkItemId(), tenantId);
        if (StaskSopCropScope.ALL.equals(bo.getCropScope())) {
            if (bo.getCropSpeciesId() != null) {
                throw new ServiceException("全部作物范围不能传作物物种");
            }
            entity.setCropSpeciesId(ALL_CROP_SPECIES_ID);
        } else {
            if (bo.getCropSpeciesId() == null || bo.getCropSpeciesId() <= 0) {
                throw new ServiceException("指定作物范围时必须选择作物物种");
            }
            assertEnabledCropSpecies(bo.getCropSpeciesId(), tenantId);
            entity.setCropSpeciesId(bo.getCropSpeciesId());
        }
        entity.setTenantId(tenantId);
        entity.setCropScope(bo.getCropScope());
        entity.setLanguage(bo.getLanguage());
        entity.setContentBlocks(contentValidator.sanitize(bo.getContentBlocks()));
    }

    private void assertEnabledWorkItem(Long workItemId, String tenantId) {
        List<SfFarmWorkDict> items = farmWorkDictMapper.selectMobileEnabledItems(tenantId);
        if (workItemId == null || CollUtil.isEmpty(items)
            || items.stream().noneMatch(item -> Objects.equals(item.getDictId(), workItemId))) {
            throw new ServiceException("农事项必须是启用的叶子项目");
        }
    }

    private void assertEnabledCropSpecies(Long cropSpeciesId, String tenantId) {
        SfCropSpecies species = cropSpeciesMapper.selectOne(Wrappers.<SfCropSpecies>lambdaQuery()
            .eq(SfCropSpecies::getTenantId, tenantId)
            .eq(SfCropSpecies::getSpeciesId, cropSpeciesId)
            .eq(SfCropSpecies::getStatus, SystemConstants.NORMAL)
            .eq(SfCropSpecies::getDelFlag, SystemConstants.NORMAL));
        if (species == null) {
            throw new ServiceException("作物物种不存在、已停用或无权使用");
        }
    }

    private SfStaskSop requireSop(Long sopId) {
        SfStaskSop sop = sopMapper.selectOne(Wrappers.<SfStaskSop>lambdaQuery()
            .eq(SfStaskSop::getSopId, sopId)
            .eq(SfStaskSop::getTenantId, requireTenantId()));
        if (sop == null) {
            throw new ServiceException("农事SOP不存在或无权访问");
        }
        return sop;
    }

    private SfStaskSop selectMatchedSop(String tenantId, Long workItemId, Long cropSpeciesId, String requestedLanguage) {
        List<SfStaskSop> candidates = cropSpeciesId == null
            ? List.of()
            : sopMapper.selectByMatchKey(tenantId, workItemId, cropSpeciesId);
        if (CollUtil.isEmpty(candidates)) {
            candidates = sopMapper.selectByMatchKey(tenantId, workItemId, ALL_CROP_SPECIES_ID);
        }
        if (CollUtil.isEmpty(candidates)) {
            return null;
        }
        return candidates.stream()
            .filter(item -> Objects.equals(requestedLanguage, item.getLanguage()))
            .findFirst()
            .orElse(candidates.stream().findFirst().orElse(null));
    }

    private Long resolveActiveCropSpeciesId(Long greenhouseId) {
        if (greenhouseId == null) {
            return null;
        }
        Map<Long, List<SfPlantingBatchVo>> batchMap = plantingBatchService.mapActiveVoByFieldIds(List.of(greenhouseId));
        List<SfPlantingBatchVo> batches = batchMap.get(greenhouseId);
        if (CollUtil.isEmpty(batches)) {
            return null;
        }
        Comparator<SfPlantingBatchVo> comparator = Comparator
            .comparing(SfPlantingBatchVo::getBatchStatus, PlantingBatchStatus.ACTIVE_PRIORITY)
            .thenComparing(SfPlantingBatchVo::getBatchId, Comparator.nullsLast(Comparator.reverseOrder()));
        return batches.stream().sorted(comparator).map(SfPlantingBatchVo::getSpeciesId)
            .filter(Objects::nonNull).findFirst().orElse(null);
    }

    private void assertOrderVisible(SfStaskWorkOrder order) {
        Long userId = LoginHelper.getUserId();
        if (EmployeeConstants.USER_TYPE_WX_EMPLOYEE.equals(LoginHelper.getLoginUser().getUserType())
            && employeeAccessor.hasAppRole(userId, EmployeeConstants.APP_ROLE_STASK_WORKER)) {
            throw messages.exception(StaskMessageKeys.ERROR_DISPATCH_FEATURE_DEPRECATED);
        }
        workOrderService.detail(order.getOrderId());
    }

    private List<SfStaskSopVo> toVos(Collection<SfStaskSop> entities, boolean includeContent) {
        return entities.stream().map(item -> toVo(item, includeContent)).collect(Collectors.toList());
    }

    private SfStaskSopVo toVo(SfStaskSop entity, boolean includeContent) {
        SfStaskSopVo vo = new SfStaskSopVo();
        BeanUtil.copyProperties(entity, vo, "contentBlocks");
        vo.setWorkItemName(workItemName(entity.getWorkItemId(), entity.getTenantId()));
        if (Objects.equals(entity.getCropSpeciesId(), ALL_CROP_SPECIES_ID)) {
            vo.setCropScope(StaskSopCropScope.ALL);
            vo.setCropSpeciesId(null);
            vo.setCropSpeciesName("全部作物");
        } else {
            vo.setCropScope(StaskSopCropScope.SPECIFIC);
            vo.setCropSpeciesName(cropSpeciesName(entity.getCropSpeciesId(), entity.getTenantId()));
        }
        if (includeContent) {
            vo.setContentBlocks(contentValidator.parse(entity.getContentBlocks()));
        }
        return vo;
    }

    private String workItemName(Long workItemId, String tenantId) {
        return farmWorkDictMapper.selectMobileEnabledItems(tenantId).stream()
            .filter(item -> Objects.equals(item.getDictId(), workItemId))
            .map(SfFarmWorkDict::getDictName).findFirst().orElse(null);
    }

    private String cropSpeciesName(Long speciesId, String tenantId) {
        if (speciesId == null) {
            return null;
        }
        SfCropSpecies species = cropSpeciesMapper.selectOne(Wrappers.<SfCropSpecies>lambdaQuery()
            .eq(SfCropSpecies::getTenantId, tenantId)
            .eq(SfCropSpecies::getSpeciesId, speciesId));
        return species == null ? null : species.getSpeciesName();
    }

    private String resolveRequestedLanguage() {
        return "ug".equalsIgnoreCase(LocaleContextHolder.getLocale().getLanguage())
            ? StaskSopLanguage.UG_CN : StaskSopLanguage.ZH_CN;
    }

    private String requireTenantId() {
        String tenantId = TenantHelper.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            throw new ServiceException("缺少租户上下文");
        }
        return tenantId;
    }

    private ServiceException duplicateSopException() {
        return new ServiceException("同一农事项、作物范围和语言的SOP已存在", HttpStatus.CONFLICT);
    }
}
