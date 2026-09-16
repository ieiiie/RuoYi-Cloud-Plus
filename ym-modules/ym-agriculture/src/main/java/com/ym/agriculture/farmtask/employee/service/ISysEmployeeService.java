package com.ym.agriculture.farmtask.employee.service;

import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeReviewBatchBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeUpdateBo;
import com.ym.agriculture.farmtask.employee.model.bo.MiniappRegisterApprovalQueryBo;
import com.ym.agriculture.farmtask.employee.model.vo.EmployeeCreateResultVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeProfileVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;

import java.util.Collection;
import java.util.List;

/**
 * 员工服务接口。
 */
public interface ISysEmployeeService {

    /**
     * 分页查询员工列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 员工分页结果
     */
    PageResult<SysEmployeeVo> queryPage(SysEmployeeBo bo, PageQuery pageQuery);

    /**
     * 分页查询审核列表。
     *
     * @param bo        查询条件
     * @param pageQuery 分页条件
     * @return 审核分页结果
     */
    PageResult<SysEmployeeVo> queryReviewPage(SysEmployeeBo bo, PageQuery pageQuery);

    /**
     * 分页查询小程序注册审批记录。
     *
     * @param bo 查询条件
     * @param pageQuery 分页条件
     * @return 注册审批分页列表
     */
    PageResult<SysEmployeeVo> queryMiniappRegisterApprovalPage(MiniappRegisterApprovalQueryBo bo,
        PageQuery pageQuery);

    /**
     * 统计当前租户待审批注册数量。
     *
     * @return 待审批数量
     */
    long countMiniappPendingRegisterApprovals();

    /**
     * 查询当前租户的注册审批详情。
     *
     * @param employeeId 申请人员ID
     * @return 注册审批人员信息
     */
    SysEmployeeVo queryMiniappRegisterApprovalById(Long employeeId);

    /**
     * 后台录入外部员工并生成4位绑定码。
     *
     * @param bo 员工信息
     * @return 员工与绑定码
     */
    EmployeeCreateResultVo createWithBindCode(SysEmployeeBo bo);

    /**
     * 根据ID查询员工。
     *
     * @param employeeId 员工ID
     * @return 员工信息
     */
    SysEmployeeVo queryById(Long employeeId);

    /**
     * 根据ID查询人员档案详情。
     *
     * @param employeeId 人员ID
     * @return 人员档案详情
     */
    SysEmployeeProfileVo queryProfileById(Long employeeId);

    /**
     * 编辑人员档案基本信息（不含微信 openid / 微信关联手机号）。
     *
     * @param bo 编辑参数
     * @return 影响行数
     */
    int updateProfile(SysEmployeeUpdateBo bo);

    /**
     * 根据ID集合批量查询员工。
     *
     * @param employeeIds 员工ID集合
     * @return 员工列表
     */
    List<SysEmployeeVo> queryByIds(Collection<Long> employeeIds);

    /**
     * 根据ID集合批量查询员工基础信息，不补充邀请码等档案扩展字段。
     *
     * @param employeeIds 员工ID集合
     * @return 员工基础信息列表
     */
    List<SysEmployeeVo> queryBasicByIds(Collection<Long> employeeIds);

    /**
     * 按应用角色查询可选员工列表。
     *
     * @param appRoleCode        应用角色编码
     * @param keyword            搜索关键字，匹配姓名或手机号
     * @param excludeEmployeeIds 需要排除的员工ID集合
     * @return 可选员工列表
     */
    List<SysEmployeeLeaderOptionVo> queryOptionsByRole(String appRoleCode, String keyword,
        Collection<Long> excludeEmployeeIds);

    /**
     * 查询 stask 农事分配可选组长列表。
     *
     * @param keyword            搜索关键字，匹配姓名或手机号
     * @param excludeEmployeeIds 需要排除的员工ID集合，通常为已参与当前大棚的组长
     * @return 可选组长列表
     */
    List<SysEmployeeLeaderOptionVo> queryLeaderOptions(String keyword, Collection<Long> excludeEmployeeIds);

    /**
     * 查询 stask 小程序可选组长列表。
     *
     * @param keyword 搜索关键字，匹配姓名或手机号
     * @param status  人员状态，不传默认正常
     * @return 可选组长列表
     */
    List<SysEmployeeLeaderOptionVo> queryLeaderOptions(String keyword, String status);

    /**
     * 判断人员是否拥有指定应用角色（仅比对 app_role_code）。
     *
     * @param employeeId  人员ID
     * @param appRoleCode 目标角色编码
     * @return 人员存在且 app_role_code 与目标一致时返回 true
     */
    boolean hasAppRole(Long employeeId, String appRoleCode);

    /**
     * 判断人员是否拥有指定应用角色（仅比对 app_role_code）。
     *
     * @param employee    人员信息，为 null 时返回 false
     * @param appRoleCode 目标角色编码
     * @return app_role_code 与目标一致时返回 true
     */
    boolean hasAppRole(SysEmployeeVo employee, String appRoleCode);

    /**
     * 根据微信 openid 查询可登录员工。
     *
     * @param openid 微信 openid
     * @return 员工信息，不存在返回 null
     */
    SysEmployeeVo queryLoginEmployeeByOpenid(String openid);

    /**
     * 根据微信 openid 查询人员记录（含待审核/已拒绝），未找到返回 null，不抛异常。
     *
     * @param openid 微信 openid
     * @return 员工信息，不存在或 openid 为空返回 null
     */
    SysEmployeeVo queryByWxOpenid(String openid);

    /**
     * 重新生成员工绑定码。
     *
     * @param employeeId 员工ID
     * @return 新绑定码
     */
    EmployeeCreateResultVo regenerateBindCode(Long employeeId);

    /**
     * 删除未绑定员工。
     *
     * @param employeeId 员工ID
     * @return 影响行数
     */
    int deleteUnboundEmployee(Long employeeId);

    /**
     * 批量删除外部人员（人员列表/审核列表通用）。
     *
     * @param employeeIds 员工ID数组
     * @return 影响行数
     */
    int deleteByIds(Long[] employeeIds);

    /**
     * 修改员工应用角色。
     *
     * @param bo 角色信息
     * @return 影响行数
     */
    int updateRole(SysEmployeeBo bo);

    /**
     * 使用4位绑定码绑定微信 openid。
     *
     * @param bo 绑定参数
     * @return 绑定后的员工
     */
    SysEmployeeVo bindByCode(SysEmployeeBo bo);

    /**
     * 使用手机号绑定微信 openid（phone 可手填，wxPhone 可选；不要求微信 getPhoneNumber 授权）。
     *
     * @param bo 绑定参数
     * @return 绑定后的员工
     */
    SysEmployeeVo bindByPhone(SysEmployeeBo bo);

    /**
     * 使用角色邀请码注册外部员工。
     *
     * @param bo 注册参数
     * @return 待审核员工
     */
    SysEmployeeVo registerByInviteCode(SysEmployeeBo bo);

    /**
     * 根据 openid 查询审核状态。
     *
     * @param openid 微信 openid
     * @return 员工信息
     */
    SysEmployeeVo checkStatus(String openid);

    /**
     * 按微信 openid 同步微信授权手机号；无记录或号码未变时不写库。
     *
     * @param openid  微信 openid
     * @param wxPhone 微信授权纯手机号
     * @return 是否更新了 wx_phone
     */
    boolean syncWxPhoneByOpenid(String openid, String wxPhone);

    /**
     * 审核外部员工注册申请。
     *
     * @param bo 审核参数
     * @return 影响行数
     */
    int review(SysEmployeeBo bo);

    /**
     * 批量审核外部员工注册申请。
     *
     * @param bo           批量审核参数
     * @param reviewResult 审核结果：1通过 2拒绝
     * @return 影响行数
     */
    int batchReview(SysEmployeeReviewBatchBo bo, String reviewResult);

    /**
     * 填充小程序登录上下文：邀请码优先按 inviteCodeId 查角色邀请码，否则取人员绑定码。
     *
     * @param employee 员工信息，为 null 时不处理
     */
    void enrichWxLoginContext(SysEmployeeVo employee);
}
