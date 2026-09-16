package com.ym.agriculture.farmtask.workorder.model.bo;

import com.ym.agriculture.farmtask.workorder.model.constants.StaskTaskScope;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * stask 小程序全部任务列表查询入参。
 */
@Data
public class SfStaskAllTaskQueryBo {

    /**
     * 任务范围；技术员默认 RELATED_TO_ME，生产管理员/领导默认 ALL。
     */
    private StaskTaskScope taskScope;

    /**
     * 发起人员工 ID；与任务范围、状态及日期条件独立组合。
     */
    private Long creatorEmployeeId;

    /**
     * 卡片类型：PACKAGE-任务包，SPLIT-拆分工单；为空表示全部类型。
     */
    private List<String> cardTypes;

    /**
     * 任务状态集合：DRAFT-草稿，PENDING_TECH_CONFIRM-待技术员确认，TECH_REJECTED-技术退回（任务包），
     * PENDING_LEADER_ACCEPT-待组长接单，ASSIGN_COMPLETE-派工完成，
     * LEADER_ARRIVED-已到达，PENDING_ACCEPTANCE-待验收，ACCEPTANCE_PASSED-验收通过，
     * ACCEPTANCE_REJECTED-验收不通过，VOIDED-作废，CANCELLED-已撤销（任务包）。
     */
    private List<String> statuses;

    /**
     * 大棚 ID 集合；为空表示全部大棚。
     */
    private List<Long> greenhouseIds;

    /**
     * 农事项目 ID 集合；为空表示全部农事项目。
     */
    private List<Long> workItemIds;

    /**
     * 计划作业开始日期，格式：yyyy-MM-dd。
     */
    private Date planStartDate;

    /**
     * 计划作业结束日期，格式：yyyy-MM-dd。
     */
    private Date planEndDate;
}
