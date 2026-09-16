package com.ym.system.saas.service;

import com.ym.system.saas.domain.vo.SaasRegionTreeVo;
import com.ym.system.saas.domain.vo.SaasRegionVo;

import java.util.List;

/** SaaS 行政区划查询服务。 */
public interface ISaasRegionService {

    /** 按父级 adcode 查询直接子级；为 null 时查询全部行政区划。 */
    List<SaasRegionVo> listChildren(Long parentId);

    /** 按 adcode 查询行政区划详情。 */
    SaasRegionVo selectByAdcode(String adcode);

    /** 按名称、完整名称或 adcode 前缀搜索行政区划。 */
    List<SaasRegionVo> search(String keyword, String regionLevel);

    /** 以指定节点为根查询整棵行政区划子树。 */
    SaasRegionTreeVo treeByRootId(Long regionId);
}
