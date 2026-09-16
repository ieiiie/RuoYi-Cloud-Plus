package com.ym.agriculture.farmtask.worker.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.worker.model.bo.SfStaskWorkerListQueryBo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerDetailVo;
import com.ym.agriculture.farmtask.worker.model.vo.SfStaskWorkerListItemVo;

/**
 * stask 小程序人员目录服务接口。
 */
public interface ISfStaskWorkerDirectoryService {

    /**
     * 生产管理员视角分页查询人员列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 人员卡片分页列表
     */
    PageResult<SfStaskWorkerListItemVo> managerWorkers(SfStaskWorkerListQueryBo bo, PageQuery pageQuery);

    /**
     * 组长视角分页查询工人列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 工人卡片分页列表
     */
    PageResult<SfStaskWorkerListItemVo> leaderWorkers(SfStaskWorkerListQueryBo bo, PageQuery pageQuery);

    /**
     * 生产管理员视角查询人员详情。
     *
     * @param employeeId 员工ID
     * @return 人员详情
     */
    SfStaskWorkerDetailVo managerWorkerDetail(Long employeeId);

    /**
     * 组长视角查询工人详情。
     *
     * @param employeeId 员工ID
     * @return 工人详情
     */
    SfStaskWorkerDetailVo leaderWorkerDetail(Long employeeId);
}
