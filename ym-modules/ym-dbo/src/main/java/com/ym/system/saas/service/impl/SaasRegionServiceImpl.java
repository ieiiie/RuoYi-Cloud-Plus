package com.ym.system.saas.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.dynamic.datasource.annotation.DS;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.system.saas.domain.SaasRegion;
import com.ym.system.saas.domain.vo.SaasRegionTreeVo;
import com.ym.system.saas.domain.vo.SaasRegionVo;
import com.ym.system.saas.mapper.SaasRegionMapper;
import com.ym.system.saas.service.ISaasRegionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** SaaS 行政区划查询服务实现。 */
@Service
@DS("saas")
@RequiredArgsConstructor
public class SaasRegionServiceImpl implements ISaasRegionService {

    private final SaasRegionMapper regionMapper;

    @Override
    public List<SaasRegionVo> listChildren(Long parentId) {
        return regionMapper.selectVoList(QueryBuilder.lambda(SaasRegion.class)
            .eq(parentId != null, SaasRegion::getParentId, parentId)
            .orderByAsc(SaasRegion::getParentId)
            .orderByAsc(SaasRegion::getSortOrder)
            .orderByAsc(SaasRegion::getRegionId)
            .build());
    }

    @Override
    public SaasRegionVo selectByAdcode(String adcode) {
        if (adcode == null || adcode.isBlank()) {
            return null;
        }
        return regionMapper.selectVoOne(QueryBuilder.lambda(SaasRegion.class)
            .eq(SaasRegion::getAdcode, adcode.trim())
            .build());
    }

    @Override
    public List<SaasRegionVo> search(String keyword, String regionLevel) {
        String normalizedKeyword = keyword.trim();
        return regionMapper.selectVoList(QueryBuilder.lambda(SaasRegion.class)
            .and(wrapper -> wrapper
                .like(SaasRegion::getRegionName, normalizedKeyword)
                .or().like(SaasRegion::getFullName, normalizedKeyword)
                .or().likeRight(SaasRegion::getAdcode, normalizedKeyword))
            .eq(regionLevel != null && !regionLevel.isBlank(), SaasRegion::getRegionLevel, regionLevel)
            .orderByAsc(SaasRegion::getParentId)
            .orderByAsc(SaasRegion::getSortOrder)
            .orderByAsc(SaasRegion::getRegionId)
            .build());
    }

    @Override
    public SaasRegionTreeVo treeByRootId(Long regionId) {
        if (regionId == null) {
            return null;
        }
        SaasRegion rootRow = regionMapper.selectById(regionId);
        if (rootRow == null) {
            return null;
        }

        List<SaasRegion> flat = new ArrayList<>();
        flat.add(rootRow);
        Set<Long> frontier = new HashSet<>();
        Long rootAdcode = parseAdcode(rootRow.getAdcode());
        if (rootAdcode != null) {
            frontier.add(rootAdcode);
        }
        while (!frontier.isEmpty()) {
            List<SaasRegion> next = regionMapper.selectList(
                Wrappers.<SaasRegion>lambdaQuery().in(SaasRegion::getParentId, frontier));
            if (next.isEmpty()) {
                break;
            }
            flat.addAll(next);
            Set<Long> nextFrontier = new HashSet<>();
            for (SaasRegion region : next) {
                if (isLeafLevel(region.getRegionLevel())) {
                    continue;
                }
                Long adcode = parseAdcode(region.getAdcode());
                if (adcode != null) {
                    nextFrontier.add(adcode);
                }
            }
            frontier = nextFrontier;
        }

        Map<Long, SaasRegionTreeVo> byId = new HashMap<>(flat.size() * 2);
        for (SaasRegion region : flat) {
            SaasRegionTreeVo node = BeanUtil.copyProperties(region, SaasRegionTreeVo.class);
            node.setChildren(new ArrayList<>());
            byId.put(region.getRegionId(), node);
        }
        SaasRegionTreeVo root = byId.get(regionId);
        for (SaasRegion region : flat) {
            if (region.getRegionId().equals(regionId)) {
                continue;
            }
            SaasRegionTreeVo parent = findDirectParentVo(region, flat, byId);
            SaasRegionTreeVo node = byId.get(region.getRegionId());
            if (parent != null && node != null) {
                parent.getChildren().add(node);
            }
        }
        if (root != null) {
            sortTreeChildren(root);
        }
        return root;
    }

    private static boolean isLeafLevel(String regionLevel) {
        return regionLevel != null && "street".equalsIgnoreCase(regionLevel.trim());
    }

    private static Long parseAdcode(String adcode) {
        if (adcode == null || adcode.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(adcode.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static SaasRegionTreeVo findDirectParentVo(
        SaasRegion child, List<SaasRegion> flat, Map<Long, SaasRegionTreeVo> byId) {
        Long parentAdcode = child.getParentId();
        if (parentAdcode == null || parentAdcode == 0L) {
            return null;
        }
        for (SaasRegion candidate : flat) {
            if (parentAdcode.equals(parseAdcode(candidate.getAdcode()))
                && isDirectParentLevel(candidate.getRegionLevel(), child.getRegionLevel())) {
                return byId.get(candidate.getRegionId());
            }
        }
        return null;
    }

    private static boolean isDirectParentLevel(String parentLevel, String childLevel) {
        if (parentLevel == null || childLevel == null) {
            return false;
        }
        return switch (childLevel.trim().toLowerCase()) {
            case "street" -> "district".equalsIgnoreCase(parentLevel.trim());
            case "district" -> "city".equalsIgnoreCase(parentLevel.trim());
            case "city" -> "province".equalsIgnoreCase(parentLevel.trim());
            default -> false;
        };
    }

    private static void sortTreeChildren(SaasRegionTreeVo node) {
        Comparator<SaasRegionTreeVo> comparator = Comparator
            .comparing(SaasRegionTreeVo::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SaasRegionTreeVo::getRegionId, Comparator.nullsLast(Comparator.naturalOrder()));
        node.getChildren().sort(comparator);
        node.getChildren().forEach(SaasRegionServiceImpl::sortTreeChildren);
    }
}
