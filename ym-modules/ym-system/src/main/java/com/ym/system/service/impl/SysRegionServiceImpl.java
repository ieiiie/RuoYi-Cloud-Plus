package com.ym.system.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.ym.common.mybatis.core.query.QueryBuilder;
import com.ym.system.domain.SysRegion;
import com.ym.system.domain.vo.SysRegionTreeVo;
import com.ym.system.domain.vo.SysRegionVo;
import com.ym.system.mapper.SysRegionMapper;
import com.ym.system.service.ISysRegionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 系统行政区划查询服务实现。 */
@Service
@RequiredArgsConstructor
public class SysRegionServiceImpl implements ISysRegionService {

    private final SysRegionMapper regionMapper;

    @Override
    public List<SysRegionVo> listChildren(Long parentId) {
        return regionMapper.selectVoList(QueryBuilder.lambda(SysRegion.class)
            .eq(parentId != null, SysRegion::getParentId, parentId)
            .orderByAsc(SysRegion::getParentId)
            .orderByAsc(SysRegion::getSortOrder)
            .orderByAsc(SysRegion::getRegionId)
            .build());
    }

    @Override
    public SysRegionVo selectByAdcode(String adcode) {
        if (adcode == null || adcode.isBlank()) {
            return null;
        }
        return regionMapper.selectVoOne(QueryBuilder.lambda(SysRegion.class)
            .eq(SysRegion::getAdcode, adcode.trim())
            .build());
    }

    @Override
    public List<SysRegionVo> search(String keyword, String regionLevel) {
        String normalizedKeyword = keyword.trim();
        return regionMapper.selectVoList(QueryBuilder.lambda(SysRegion.class)
            .and(wrapper -> wrapper
                .like(SysRegion::getRegionName, normalizedKeyword)
                .or().like(SysRegion::getFullName, normalizedKeyword)
                .or().likeRight(SysRegion::getAdcode, normalizedKeyword))
            .eq(regionLevel != null && !regionLevel.isBlank(), SysRegion::getRegionLevel, regionLevel)
            .orderByAsc(SysRegion::getParentId)
            .orderByAsc(SysRegion::getSortOrder)
            .orderByAsc(SysRegion::getRegionId)
            .build());
    }

    @Override
    public SysRegionTreeVo treeByRootId(Long regionId) {
        if (regionId == null) {
            return null;
        }
        SysRegion rootRow = regionMapper.selectById(regionId);
        if (rootRow == null) {
            return null;
        }

        List<SysRegion> flat = new ArrayList<>();
        flat.add(rootRow);
        Set<Long> frontier = new HashSet<>();
        Long rootAdcode = parseAdcode(rootRow.getAdcode());
        if (rootAdcode != null) {
            frontier.add(rootAdcode);
        }
        while (!frontier.isEmpty()) {
            List<SysRegion> next = regionMapper.selectList(
                Wrappers.<SysRegion>lambdaQuery().in(SysRegion::getParentId, frontier));
            if (next.isEmpty()) {
                break;
            }
            flat.addAll(next);
            Set<Long> nextFrontier = new HashSet<>();
            for (SysRegion region : next) {
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

        Map<Long, SysRegionTreeVo> byId = new HashMap<>(flat.size() * 2);
        for (SysRegion region : flat) {
            SysRegionTreeVo node = BeanUtil.copyProperties(region, SysRegionTreeVo.class);
            node.setChildren(new ArrayList<>());
            byId.put(region.getRegionId(), node);
        }
        SysRegionTreeVo root = byId.get(regionId);
        for (SysRegion region : flat) {
            if (region.getRegionId().equals(regionId)) {
                continue;
            }
            SysRegionTreeVo parent = findDirectParentVo(region, flat, byId);
            SysRegionTreeVo node = byId.get(region.getRegionId());
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

    private static SysRegionTreeVo findDirectParentVo(
        SysRegion child, List<SysRegion> flat, Map<Long, SysRegionTreeVo> byId) {
        Long parentAdcode = child.getParentId();
        if (parentAdcode == null || parentAdcode == 0L) {
            return null;
        }
        for (SysRegion candidate : flat) {
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

    private static void sortTreeChildren(SysRegionTreeVo node) {
        Comparator<SysRegionTreeVo> comparator = Comparator
            .comparing(SysRegionTreeVo::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SysRegionTreeVo::getRegionId, Comparator.nullsLast(Comparator.naturalOrder()));
        node.getChildren().sort(comparator);
        node.getChildren().forEach(SysRegionServiceImpl::sortTreeChildren);
    }
}
