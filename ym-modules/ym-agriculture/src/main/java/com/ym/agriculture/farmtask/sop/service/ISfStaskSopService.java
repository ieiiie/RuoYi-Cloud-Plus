package com.ym.agriculture.farmtask.sop.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopBo;
import com.ym.agriculture.farmtask.sop.model.bo.SfStaskSopQueryBo;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopResolveVo;
import com.ym.agriculture.farmtask.sop.model.vo.SfStaskSopVo;

/** 农事 SOP 管理与解析服务。 */
public interface ISfStaskSopService {

    /** 分页查询当前租户 SOP。 */
    PageResult<SfStaskSopVo> page(SfStaskSopQueryBo bo, PageQuery pageQuery);

    /** 查询 SOP 详情。 */
    SfStaskSopVo getById(Long sopId);

    /** 新增 SOP。 */
    Long add(SfStaskSopBo bo);

    /** 编辑 SOP。 */
    void update(Long sopId, SfStaskSopBo bo);

    /** 删除 SOP。 */
    void remove(Long sopId);

    /** 复制为未落库的新建草稿。 */
    SfStaskSopVo copyDraft(Long sopId);

    /** 解析当前用户可读取的拆分工单 SOP。 */
    SfStaskSopResolveVo resolveForMiniapp(Long orderId);
}
