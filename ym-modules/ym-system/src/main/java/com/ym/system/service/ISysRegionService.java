package com.ym.system.service;

import com.ym.system.domain.vo.SysRegionVo;
import com.ym.system.domain.vo.SysRegionTreeVo;

import java.util.List;

/** 系统行政区划查询服务。 */
public interface ISysRegionService {

    /**
     * 查询行政区划。
     *
     * @param parentId 父级 adcode；为 null 时查询全部行政区划
     * @return 行政区划列表
     */
    List<SysRegionVo> listChildren(Long parentId);

    /** 按 adcode 查询行政区划详情。 */
    SysRegionVo selectByAdcode(String adcode);

    /** 按名称、完整名称或 adcode 前缀搜索行政区划。 */
    List<SysRegionVo> search(String keyword, String regionLevel);

    /** 以指定节点为根查询整棵行政区划子树。 */
    SysRegionTreeVo treeByRootId(Long regionId);
}
