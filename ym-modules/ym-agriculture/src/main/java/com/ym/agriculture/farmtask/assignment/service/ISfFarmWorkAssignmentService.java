package com.ym.agriculture.farmtask.assignment.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignBatchBo;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignCancelBatchBo;
import com.ym.agriculture.farmtask.assignment.model.bo.SfFarmAssignPageBo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignDetailVo;
import com.ym.agriculture.farmtask.assignment.model.vo.SfFarmAssignGreenhouseVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;

import java.util.List;

/**
 * stask 农事分配服务接口。
 */
public interface ISfFarmWorkAssignmentService {

    /**
     * 分页查询大棚农事分配统计。
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 大棚分配统计分页
     */
    PageResult<SfFarmAssignGreenhouseVo> queryGreenhousePage(SfFarmAssignPageBo bo, PageQuery pageQuery);

    /**
     * 查询大棚农事分配详情。
     *
     * @param greenhouseId 大棚ID
     * @return 分配详情
     */
    SfFarmAssignDetailVo queryDetail(Long greenhouseId);

    /**
     * 批量分配农事项给同一组长。
     *
     * @param bo 分配入参
     * @return 新增记录数
     */
    int batchAssign(SfFarmAssignBatchBo bo);

    /**
     * 取消单条农事分配。
     *
     * @param assignmentId 分配记录ID
     * @return 删除记录数
     */
    int remove(Long assignmentId);

    /**
     * 批量取消指定组长在大棚下的农事分配。
     *
     * @param bo 批量取消入参
     * @return 删除记录数
     */
    int batchRemove(SfFarmAssignCancelBatchBo bo);

    /**
     * 查询当前大棚可选组长列表，过滤已参与本棚的组长。
     *
     * @param greenhouseId 大棚ID
     * @param keyword      搜索关键字
     * @return 可选组长列表
     */
    List<SysEmployeeLeaderOptionVo> queryLeaderOptions(Long greenhouseId, String keyword);
}
