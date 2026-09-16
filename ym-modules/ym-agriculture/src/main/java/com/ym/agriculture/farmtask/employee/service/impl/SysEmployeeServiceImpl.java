package com.ym.agriculture.farmtask.employee.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdcardUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.satoken.utils.LoginHelper;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.dao.SysEmployeeMapper;
import com.ym.agriculture.farmtask.employee.dao.SysInviteCodeMapper;
import com.ym.agriculture.farmtask.employee.event.SysEmployeeTextChangedPublisher;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeBo;
import com.ym.agriculture.farmtask.employee.model.bo.MiniappRegisterApprovalQueryBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeReviewBatchBo;
import com.ym.agriculture.farmtask.employee.model.bo.SysEmployeeUpdateBo;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import com.ym.agriculture.farmtask.employee.model.vo.EmployeeCreateResultVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeLeaderOptionVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeProfileVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysInviteCodeVo;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import com.ym.agriculture.farmtask.employee.service.EmployeeMiniappApprovalPermissionService;
import com.ym.agriculture.farmtask.employee.service.ISysEmployeeService;
import com.ym.agriculture.farmtask.employee.service.ISysInviteCodeService;
import com.ym.agriculture.farmtask.employee.support.EmployeeRoleChangeGuard;
import com.ym.system.api.RemoteUserService;
import com.ym.system.api.domain.bo.RemoteUserProfileUpdateBo;
import lombok.RequiredArgsConstructor;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * 员工服务实现。
 */
@RequiredArgsConstructor
@Service
public class SysEmployeeServiceImpl implements ISysEmployeeService {

    private static final List<String> PROFILE_READONLY_FIELDS = List.of("wxOpenid", "wxPhone");

    private final SysEmployeeMapper baseMapper;
    private final SysInviteCodeMapper inviteCodeMapper;
    private final ISysInviteCodeService inviteCodeService;
    private final IEmployeeAppRoleService employeeAppRoleService;
    private final EmployeeMiniappApprovalPermissionService miniappApprovalPermissionService;
    private final SysEmployeeTextChangedPublisher employeeTextChangedPublisher;

    @DubboReference
    private RemoteUserService remoteUserService;

    @Autowired
    private List<EmployeeRoleChangeGuard> roleChangeGuards = List.of();

    @Value("${wechat.employee-miniprogram.employee.auto-approve:false}")
    private boolean employeeAutoApprove;

    private static final String WX_OPENID_APPROVED_MESSAGE = "当前微信已绑定其他租户人员，请退出当前账号或联系管理员处理";

    @Override
    public PageResult<SysEmployeeVo> queryPage(SysEmployeeBo bo, PageQuery pageQuery) {
        Page<SysEmployeeVo> page = baseMapper.selectPageEmployeeList(pageQuery.build(), buildQueryWrapper(bo, false));
        enrichEmployeeVos(page.getRecords());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public PageResult<SysEmployeeVo> queryReviewPage(SysEmployeeBo bo, PageQuery pageQuery) {
        Page<SysEmployeeVo> page = baseMapper.selectPageEmployeeList(pageQuery.build(), buildQueryWrapper(bo, true));
        enrichEmployeeVos(page.getRecords());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public PageResult<SysEmployeeVo> queryMiniappRegisterApprovalPage(MiniappRegisterApprovalQueryBo bo,
        PageQuery pageQuery) {
        MiniappRegisterApprovalQueryBo query = bo == null ? new MiniappRegisterApprovalQueryBo() : bo;
        String reviewStatus = resolveReviewStatus(query.getAuditStatus());
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .isNotNull(SysEmployee::getReviewStatus)
            .eq(StringUtils.isNotBlank(reviewStatus), SysEmployee::getReviewStatus, reviewStatus)
            .eq(StringUtils.isNotBlank(query.getApplyAppRoleCode()), SysEmployee::getAppRoleCode,
                query.getApplyAppRoleCode())
            .and(StringUtils.isNotBlank(query.getKeyword()), condition -> condition
                .like(SysEmployee::getName, query.getKeyword()).or().like(SysEmployee::getPhone, query.getKeyword()))
            .orderByDesc(SysEmployee::getCreateTime);
        Page<SysEmployeeVo> page = baseMapper.selectVoPage(pageQuery.build(), wrapper);
        enrichEmployeeVos(page.getRecords());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    public long countMiniappPendingRegisterApprovals() {
        return baseMapper.selectCount(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .eq(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_PENDING));
    }

    @Override
    public SysEmployeeVo queryMiniappRegisterApprovalById(Long employeeId) {
        SysEmployeeVo employee = baseMapper.selectVoOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getEmployeeId, employeeId)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .isNotNull(SysEmployee::getReviewStatus));
        if (employee == null) {
            throw new ServiceException("注册申请不存在");
        }
        enrichEmployeeVos(List.of(employee));
        return employee;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeCreateResultVo createWithBindCode(SysEmployeeBo bo) {
        if (existsPhone(bo.getPhone(), null)) {
            throw new ServiceException("手机号已存在");
        }
        SysEmployee employee = MapstructUtils.convert(bo, SysEmployee.class);
        String tenantId = StringUtils.blankToDefault(employee.getTenantId(), TenantHelper.getTenantId());
        List<Long> miniappRoleIds = resolveMiniappRoleIds(bo.getMiniappRoleIds(),
            bo.getMiniappRegisterApprovalRoleId());
        miniappApprovalPermissionService.validateBindings(employee.getAppRoleCode(), tenantId, miniappRoleIds);
        employee.setMiniappRegisterApprovalRoleId(EmployeeConstants.APP_ROLE_STASK_LEADER.equals(employee.getAppRoleCode())
            ? miniappRoleIds.stream().findFirst().orElse(null) : null);
        employee.setPersonType(EmployeeConstants.PERSON_TYPE_EXTERNAL);
        employee.setStatus(SystemConstants.NORMAL);
        employee.setReviewStatus(EmployeeConstants.REVIEW_APPROVED);
        employee.setReviewBy(LoginHelper.getUserId());
        employee.setReviewTime(new Date());
        employee.setDelFlag(SystemConstants.NORMAL);
        employee.setGender(StringUtils.blankToDefault(employee.getGender(), "2"));
        baseMapper.insert(employee);
        miniappApprovalPermissionService.replaceBindings(employee.getEmployeeId(), tenantId,
            employee.getAppRoleCode(), miniappRoleIds);
        employeeTextChangedPublisher.publishAfterCommit(
            StringUtils.blankToDefault(employee.getTenantId(), TenantHelper.getTenantId()),
            employee.getEmployeeId(), employee.getAppRoleCode());

        SysInviteCodeVo inviteCode = inviteCodeService.generateEmployeeBindCode(employee.getEmployeeId());
        SysEmployeeVo employeeVo = baseMapper.selectVoById(employee.getEmployeeId());
        enrichEmployeeVos(List.of(employeeVo));
        EmployeeCreateResultVo result = new EmployeeCreateResultVo();
        result.setEmployee(employeeVo);
        result.setInviteCode(inviteCode);
        return result;
    }

    @Override
    public SysEmployeeVo queryById(Long employeeId) {
        SysEmployeeVo vo = baseMapper.selectVoById(employeeId);
        if (ObjectUtil.isNotNull(vo)) {
            enrichEmployeeVos(List.of(vo));
        }
        return vo;
    }

    @Override
    public SysEmployeeProfileVo queryProfileById(Long employeeId) {
        SysEmployeeVo employee = queryById(employeeId);
        if (employee == null) {
            return null;
        }
        SysEmployeeProfileVo profile = BeanUtil.copyProperties(employee, SysEmployeeProfileVo.class);
        profile.setReadonlyFields(PROFILE_READONLY_FIELDS);
        if (isProfileEditable(employee.getReviewStatus())) {
            profile.setEditable(true);
            profile.setReadonlyReason(null);
        } else {
            profile.setEditable(false);
            profile.setReadonlyReason("待审核或已拒绝人员不可编辑档案");
        }
        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateProfile(SysEmployeeUpdateBo bo) {
        SysEmployee employee = requireEmployee(bo.getEmployeeId());
        validateProfileEditable(employee);
        employeeAppRoleService.requireValidRoleCode(bo.getAppRoleCode());
        List<Long> miniappRoleIds = resolveMiniappRoleIds(bo.getMiniappRoleIds(),
            bo.getMiniappRegisterApprovalRoleId());
        miniappApprovalPermissionService.validateBindings(bo.getAppRoleCode(), employee.getTenantId(), miniappRoleIds);
        validatePersonTypeChange(employee, bo.getPersonType());

        String phone = StringUtils.trim(bo.getPhone());
        if (StringUtils.isNotBlank(phone)) {
            if (phone.length() != 11) {
                throw new ServiceException("手机号必须为11位");
            }
            if (existsPhone(phone, employee.getEmployeeId())) {
                throw new ServiceException("手机号已存在");
            }
        } else {
            phone = null;
        }

        String idCard = StringUtils.trim(bo.getIdCard());
        String gender = StringUtils.blankToDefault(bo.getGender(), "2");
        Date birthDate = bo.getBirthDate();
        if (StringUtils.isNotBlank(idCard)) {
            if (idCard.length() != 18) {
                throw new ServiceException("身份证号必须为18位");
            }
            if (!IdcardUtil.isValidCard(idCard)) {
                throw new ServiceException("身份证号格式不正确");
            }
            gender = mapGenderFromIdCard(idCard);
            birthDate = IdcardUtil.getBirthDate(idCard);
        } else {
            idCard = null;
        }
        validateRoleChange(employee, bo.getAppRoleCode());

        LambdaUpdateWrapper<SysEmployee> wrapper = new LambdaUpdateWrapper<SysEmployee>()
            .set(SysEmployee::getName, bo.getName().trim())
            .set(SysEmployee::getPhone, phone)
            .set(SysEmployee::getIdCard, idCard)
            .set(SysEmployee::getPersonType, bo.getPersonType())
            .set(SysEmployee::getStatus, bo.getStatus())
            .set(SysEmployee::getGender, gender)
            .set(SysEmployee::getBirthDate, birthDate)
            .set(SysEmployee::getPhoto, bo.getPhoto())
            .set(SysEmployee::getAppRoleCode, bo.getAppRoleCode())
            .set(SysEmployee::getMiniappRegisterApprovalRoleId,
                EmployeeConstants.APP_ROLE_STASK_LEADER.equals(bo.getAppRoleCode())
                    ? miniappRoleIds.stream().findFirst().orElse(null) : null)
            .set(SysEmployee::getRemark, bo.getRemark())
            .eq(SysEmployee::getEmployeeId, employee.getEmployeeId());
        int rows = baseMapper.update(null, wrapper);
        syncUserProfileIfNeeded(employee, bo.getName().trim(), phone, gender, bo.getStatus());
        if (rows > 0) {
            miniappApprovalPermissionService.replaceBindings(employee.getEmployeeId(), employee.getTenantId(),
                bo.getAppRoleCode(), miniappRoleIds);
            employeeTextChangedPublisher.publishAfterCommit(
                StringUtils.blankToDefault(employee.getTenantId(), TenantHelper.getTenantId()),
                employee.getEmployeeId(), bo.getAppRoleCode());
        }
        return rows;
    }

    @Override
    public List<SysEmployeeVo> queryByIds(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return List.of();
        }
        List<SysEmployeeVo> rows = baseMapper.selectVoList(new LambdaQueryWrapper<SysEmployee>()
            .in(SysEmployee::getEmployeeId, employeeIds)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
        enrichEmployeeVos(rows);
        return rows;
    }

    @Override
    public List<SysEmployeeVo> queryBasicByIds(Collection<Long> employeeIds) {
        if (CollUtil.isEmpty(employeeIds)) {
            return List.of();
        }
        return baseMapper.selectVoList(new LambdaQueryWrapper<SysEmployee>()
            .in(SysEmployee::getEmployeeId, employeeIds)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
    }

    @Override
    public List<SysEmployeeLeaderOptionVo> queryOptionsByRole(String appRoleCode, String keyword,
        Collection<Long> excludeEmployeeIds) {
        return queryOptionsByRole(appRoleCode, keyword, excludeEmployeeIds, SystemConstants.NORMAL);
    }

    private List<SysEmployeeLeaderOptionVo> queryOptionsByRole(String appRoleCode, String keyword,
        Collection<Long> excludeEmployeeIds, String status) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getAppRoleCode, appRoleCode)
            .eq(StringUtils.isNotBlank(status), SysEmployee::getStatus, status)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .and(w -> w.isNull(SysEmployee::getReviewStatus)
                .or().eq(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_APPROVED))
            .and(StringUtils.isNotBlank(keyword), w -> w.like(SysEmployee::getName, keyword)
                .or().like(SysEmployee::getPhone, keyword))
            .notIn(CollUtil.isNotEmpty(excludeEmployeeIds), SysEmployee::getEmployeeId, excludeEmployeeIds)
            .orderByAsc(SysEmployee::getName)
            .orderByDesc(SysEmployee::getCreateTime);
        return MapstructUtils.convert(baseMapper.selectList(wrapper), SysEmployeeLeaderOptionVo.class);
    }

    @Override
    public List<SysEmployeeLeaderOptionVo> queryLeaderOptions(String keyword, Collection<Long> excludeEmployeeIds) {
        return queryOptionsByRole(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER, keyword, excludeEmployeeIds);
    }

    @Override
    public List<SysEmployeeLeaderOptionVo> queryLeaderOptions(String keyword, String status) {
        return queryOptionsByRole(EmployeeConstants.APP_ROLE_STASK_GROUP_LEADER, keyword, List.of(),
            StringUtils.blankToDefault(status, SystemConstants.NORMAL));
    }

    @Override
    public boolean hasAppRole(Long employeeId, String appRoleCode) {
        if (ObjectUtil.isNull(employeeId) || StringUtils.isBlank(appRoleCode)) {
            return false;
        }
        SysEmployeeVo employee = baseMapper.selectVoById(employeeId);
        return hasAppRole(employee, appRoleCode);
    }

    @Override
    public boolean hasAppRole(SysEmployeeVo employee, String appRoleCode) {
        if (ObjectUtil.isNull(employee) || StringUtils.isBlank(appRoleCode)) {
            return false;
        }
        return StringUtils.equals(appRoleCode, employee.getAppRoleCode());
    }

    @Override
    public SysEmployeeVo queryLoginEmployeeByOpenid(String openid) {
        if (StringUtils.isBlank(openid)) {
            return null;
        }
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getWxOpenid, openid)
            .eq(SysEmployee::getStatus, SystemConstants.NORMAL)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .and(w -> w.isNull(SysEmployee::getReviewStatus)
                .or().eq(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_APPROVED)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EmployeeCreateResultVo regenerateBindCode(Long employeeId) {
        SysEmployee employee = requireEmployee(employeeId);
        if (StringUtils.isNotBlank(employee.getWxOpenid())) {
            throw new ServiceException("已绑定人员不能重新生成绑定码");
        }
        inviteCodeMapper.update(null, new LambdaUpdateWrapper<SysInviteCode>()
            .set(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_DISABLED)
            .eq(SysInviteCode::getEmployeeId, employeeId)
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_EMPLOYEE));
        SysInviteCodeVo inviteCode = inviteCodeService.generateEmployeeBindCode(employeeId);
        SysEmployeeVo employeeVo = baseMapper.selectVoById(employeeId);
        enrichEmployeeVos(List.of(employeeVo));
        EmployeeCreateResultVo result = new EmployeeCreateResultVo();
        result.setEmployee(employeeVo);
        result.setInviteCode(inviteCode);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteUnboundEmployee(Long employeeId) {
        SysEmployee employee = requireEmployee(employeeId);
        if (StringUtils.isNotBlank(employee.getWxOpenid())) {
            throw new ServiceException("仅未绑定人员可删除");
        }
        return removeEmployee(employee);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] employeeIds) {
        if (ArrayUtil.isEmpty(employeeIds)) {
            throw new ServiceException("请选择要删除的人员");
        }
        List<Long> ids = CollUtil.distinct(Arrays.asList(employeeIds));
        List<SysEmployee> employees = baseMapper.selectBatchIds(ids);
        Map<Long, SysEmployee> employeeMap = employees.stream()
            .collect(Collectors.toMap(SysEmployee::getEmployeeId, e -> e, (a, b) -> a, HashMap::new));
        for (Long employeeId : ids) {
            SysEmployee employee = employeeMap.get(employeeId);
            if (ObjectUtil.isNull(employee)) {
                throw new ServiceException("人员不存在");
            }
            if (EmployeeConstants.PERSON_TYPE_INTERNAL.equals(employee.getPersonType())) {
                throw new ServiceException("人员[" + employee.getName() + "]为内部人员，不能删除");
            }
        }
        int count = 0;
        for (Long employeeId : ids) {
            count += removeEmployee(employeeMap.get(employeeId));
        }
        return count;
    }

    private int removeEmployee(SysEmployee employee) {
        miniappApprovalPermissionService.removeBindings(employee.getEmployeeId());
        inviteCodeMapper.update(null, new LambdaUpdateWrapper<SysInviteCode>()
            .set(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_DISABLED)
            .eq(SysInviteCode::getEmployeeId, employee.getEmployeeId())
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_EMPLOYEE));
        return baseMapper.deleteById(employee.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRole(SysEmployeeBo bo) {
        employeeAppRoleService.requireValidRoleCode(bo.getAppRoleCode());
        SysEmployee employee = requireEmployee(bo.getEmployeeId());
        validateRoleChange(employee, bo.getAppRoleCode());
        employee.setAppRoleCode(bo.getAppRoleCode());
        if (!EmployeeConstants.APP_ROLE_STASK_LEADER.equals(bo.getAppRoleCode())) {
            employee.setMiniappRegisterApprovalRoleId(null);
        }
        int rows = baseMapper.updateById(employee);
        if (rows > 0) {
            employeeTextChangedPublisher.publishAfterCommit(
                StringUtils.blankToDefault(employee.getTenantId(), TenantHelper.getTenantId()),
                employee.getEmployeeId(), employee.getAppRoleCode());
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysEmployeeVo bindByCode(SysEmployeeBo bo) {
        if (StringUtils.isBlank(bo.getWxOpenid())) {
            throw new ServiceException("微信 openid 不能为空");
        }
        SysInviteCode code = inviteCodeMapper.selectOne(new LambdaQueryWrapper<SysInviteCode>()
            .eq(SysInviteCode::getInviteCode, bo.getInviteCode())
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_EMPLOYEE)
            .eq(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_VALID)
            .eq(SysInviteCode::getUsedCount, 0)
            .eq(SysInviteCode::getDelFlag, SystemConstants.NORMAL));
        if (ObjectUtil.isNull(code)) {
            throw new ServiceException("邀请码不存在或已失效");
        }
        SysEmployee employee = requireEmployee(code.getEmployeeId());
        ensureEmployeeCanBind(employee);
        employee.setWxOpenid(bo.getWxOpenid());
        if (StringUtils.isNotBlank(bo.getWxPhone())) {
            employee.setWxPhone(bo.getWxPhone());
        }
        employee.setInviteCodeId(code.getCodeId());
        employee.setStatus(SystemConstants.NORMAL);
        baseMapper.updateById(employee);
        consumeInviteCode(code.getCodeId());
        return queryById(employee.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysEmployeeVo bindByPhone(SysEmployeeBo bo) {
        if (StringUtils.isBlank(bo.getWxOpenid())) {
            throw new ServiceException("微信 openid 不能为空");
        }
        String phone = StringUtils.blankToDefault(bo.getWxPhone(), bo.getPhone());
        if (StringUtils.isBlank(phone)) {
            throw new ServiceException("手机号不能为空");
        }
        SysEmployee employee = baseMapper.selectOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getPhone, phone)
            .eq(SysEmployee::getStatus, SystemConstants.NORMAL)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
        if (ObjectUtil.isNull(employee)) {
            throw new ServiceException("未找到与您手机号关联的人员信息，请使用邀请码绑定");
        }
        ensureEmployeeCanBind(employee);
        employee.setWxOpenid(bo.getWxOpenid());
        if (StringUtils.isNotBlank(bo.getWxPhone())) {
            employee.setWxPhone(bo.getWxPhone());
        }
        baseMapper.updateById(employee);
        SysInviteCode bindCode = inviteCodeMapper.selectOne(new LambdaQueryWrapper<SysInviteCode>()
            .eq(SysInviteCode::getEmployeeId, employee.getEmployeeId())
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_EMPLOYEE)
            .eq(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_VALID)
            .last("limit 1"));
        if (ObjectUtil.isNotNull(bindCode)) {
            consumeInviteCode(bindCode.getCodeId());
        }
        return queryById(employee.getEmployeeId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysEmployeeVo registerByInviteCode(SysEmployeeBo bo) {
        normalizeRegisterPhone(bo);
        ensureWxOpenidNotAlreadyApproved(bo.getWxOpenid());
        SysInviteCode code = inviteCodeService.requireValidRoleInviteCode(bo.getInviteCode(), bo.getWxOpenid());
        SysEmployee employee = baseMapper.selectOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getWxOpenid, bo.getWxOpenid())
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
        if (ObjectUtil.isNull(employee)) {
            employee = MapstructUtils.convert(bo, SysEmployee.class);
            employee.setInviteCodeId(code.getCodeId());
            employee.setPersonType(code.getPersonType());
            employee.setAppRoleCode(code.getAppRoleCode());
            employee.setTenantId(code.getTenantId());
            applyRegisterReviewState(employee);
            employee.setDelFlag(SystemConstants.NORMAL);
            employee.setGender(StringUtils.blankToDefault(employee.getGender(), "2"));
            baseMapper.insert(employee);
        } else {
            if (SystemConstants.NORMAL.equals(employee.getStatus())
                && EmployeeConstants.REVIEW_APPROVED.equals(employee.getReviewStatus())) {
                throw new ServiceException(WX_OPENID_APPROVED_MESSAGE);
            }
            updateEmployeeForReRegister(employee.getEmployeeId(), bo, code);
        }
        if (employeeAutoApprove) {
            consumeRoleInviteCode(code.getCodeId());
        }
        employeeTextChangedPublisher.publishAfterCommit(code.getTenantId(), employee.getEmployeeId(),
            code.getAppRoleCode());
        return queryById(employee.getEmployeeId());
    }

    @Override
    public SysEmployeeVo queryByWxOpenid(String openid) {
        if (StringUtils.isBlank(openid)) {
            return null;
        }
        return baseMapper.selectVoOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getWxOpenid, openid)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
    }

    @Override
    public void enrichWxLoginContext(SysEmployeeVo employee) {
        if (ObjectUtil.isNull(employee)) {
            return;
        }
        enrichEmployeeVos(List.of(employee));
    }

    @Override
    public SysEmployeeVo checkStatus(String openid) {
        if (StringUtils.isBlank(openid)) {
            throw new ServiceException("微信 openid 不能为空");
        }
        SysEmployeeVo employee = queryByWxOpenid(openid);
        if (ObjectUtil.isNull(employee)) {
            throw new ServiceException("未找到人员注册信息");
        }
        enrichEmployeeVos(List.of(employee));
        return employee;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean syncWxPhoneByOpenid(String openid, String wxPhone) {
        if (StringUtils.isBlank(openid) || StringUtils.isBlank(wxPhone)) {
            return false;
        }
        SysEmployee employee = baseMapper.selectOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getWxOpenid, openid)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
        if (ObjectUtil.isNull(employee)) {
            return false;
        }
        if (StringUtils.equals(wxPhone, employee.getWxPhone())) {
            return false;
        }
        String previousWxPhone = employee.getWxPhone();
        employee.setWxPhone(wxPhone);
        if (StringUtils.isBlank(employee.getPhone())
            || StringUtils.equals(employee.getPhone(), previousWxPhone)) {
            employee.setPhone(wxPhone);
        }
        baseMapper.updateById(employee);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int review(SysEmployeeBo bo) {
        SysEmployee employee = requireEmployee(bo.getEmployeeId());
        return applyReview(employee, bo.getReviewResult(), bo.getReviewRemark());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchReview(SysEmployeeReviewBatchBo bo, String reviewResult) {
        List<Long> employeeIds = CollUtil.distinct(bo.getEmployeeIds());
        List<SysEmployee> employees = baseMapper.selectBatchIds(employeeIds);
        Map<Long, SysEmployee> employeeMap = employees.stream()
            .collect(Collectors.toMap(SysEmployee::getEmployeeId, e -> e, (a, b) -> a, HashMap::new));
        for (Long employeeId : employeeIds) {
            SysEmployee employee = employeeMap.get(employeeId);
            if (ObjectUtil.isNull(employee)) {
                throw new ServiceException("人员不存在");
            }
            validateReviewable(employee, reviewResult);
        }
        int count = 0;
        for (Long employeeId : employeeIds) {
            count += applyReview(employeeMap.get(employeeId), reviewResult, bo.getReviewRemark());
        }
        return count;
    }

    private int applyReview(SysEmployee employee, String reviewResult, String reviewRemark) {
        if (!EmployeeConstants.REVIEW_PENDING.equals(employee.getReviewStatus())) {
            throw new ServiceException("当前人员不是待审核状态");
        }
        employee.setReviewBy(LoginHelper.getUserId());
        employee.setReviewTime(new Date());
        employee.setReviewRemark(reviewRemark);
        if (EmployeeConstants.REVIEW_APPROVED.equals(reviewResult)) {
            if (ObjectUtil.isNull(employee.getInviteCodeId())) {
                throw new ServiceException("人员未关联邀请码，无法审核通过");
            }
            employee.setStatus(SystemConstants.NORMAL);
            employee.setReviewStatus(EmployeeConstants.REVIEW_APPROVED);
            inviteCodeMapper.update(null, new LambdaUpdateWrapper<SysInviteCode>()
                .setSql("used_count = used_count + 1")
                .eq(SysInviteCode::getCodeId, employee.getInviteCodeId()));
        } else {
            employee.setStatus(SystemConstants.DISABLE);
            employee.setReviewStatus(EmployeeConstants.REVIEW_REJECTED);
        }
        return baseMapper.updateById(employee);
    }

    private void validateReviewable(SysEmployee employee, String reviewResult) {
        if (!EmployeeConstants.REVIEW_PENDING.equals(employee.getReviewStatus())) {
            throw new ServiceException("人员[" + employee.getName() + "]不是待审核状态");
        }
        if (EmployeeConstants.REVIEW_APPROVED.equals(reviewResult) && ObjectUtil.isNull(employee.getInviteCodeId())) {
            throw new ServiceException("人员[" + employee.getName() + "]未关联邀请码，无法审核通过");
        }
    }

    private LambdaQueryWrapper<SysEmployee> buildQueryWrapper(SysEmployeeBo bo, boolean reviewOnly) {
        LambdaQueryWrapper<SysEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .eq(StringUtils.isNotBlank(bo.getStatus()), SysEmployee::getStatus, bo.getStatus())
            .eq(reviewOnly && StringUtils.isNotBlank(bo.getReviewStatus()), SysEmployee::getReviewStatus, bo.getReviewStatus())
            .eq(StringUtils.isNotBlank(bo.getAppRoleCode()), SysEmployee::getAppRoleCode, bo.getAppRoleCode())
            .eq(StringUtils.isNotBlank(bo.getPersonType()), SysEmployee::getPersonType, bo.getPersonType())
            .and(StringUtils.isNotBlank(bo.getKeyword()), w -> w.like(SysEmployee::getName, bo.getKeyword())
                .or().like(SysEmployee::getPhone, bo.getKeyword()))
            .orderByDesc(SysEmployee::getCreateTime);
        if (EmployeeConstants.BIND_STATUS_UNBOUND.equals(bo.getBindStatus())) {
            wrapper.isNull(SysEmployee::getWxOpenid);
        } else if (EmployeeConstants.BIND_STATUS_BOUND.equals(bo.getBindStatus())) {
            wrapper.isNotNull(SysEmployee::getWxOpenid);
        }
        if (reviewOnly) {
            // 审核列表：仅展示待审核、已拒绝；已通过人员归入人员列表
            wrapper.isNotNull(SysEmployee::getReviewStatus)
                .ne(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_APPROVED);
        } else {
            // 人员列表仅展示可用人员：后台录入（无审核状态）或审核通过
            wrapper.and(w -> w.isNull(SysEmployee::getReviewStatus)
                .or().eq(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_APPROVED));
        }
        return wrapper;
    }

    private String resolveReviewStatus(String auditStatus) {
        String normalized = StringUtils.blankToDefault(auditStatus, "PENDING").trim().toUpperCase();
        return switch (normalized) {
            case "PENDING" -> EmployeeConstants.REVIEW_PENDING;
            case "APPROVED" -> EmployeeConstants.REVIEW_APPROVED;
            case "REJECTED" -> EmployeeConstants.REVIEW_REJECTED;
            case "ALL" -> null;
            default -> throw new ServiceException("审批状态参数不正确");
        };
    }

    private SysEmployee requireEmployee(Long employeeId) {
        SysEmployee employee = baseMapper.selectById(employeeId);
        if (ObjectUtil.isNull(employee)) {
            throw new ServiceException("人员不存在");
        }
        return employee;
    }

    private void validateRoleChange(SysEmployee employee, String targetRoleCode) {
        for (EmployeeRoleChangeGuard guard : roleChangeGuards) {
            guard.validateRoleChange(employee, targetRoleCode);
        }
    }

    private void ensureEmployeeCanBind(SysEmployee employee) {
        if (!SystemConstants.NORMAL.equals(employee.getStatus())) {
            throw new ServiceException("人员信息异常，请联系管理员");
        }
        if (StringUtils.isNotBlank(employee.getWxOpenid())) {
            throw new ServiceException("该人员已绑定其他微信账号");
        }
    }

    /**
     * 注册时补齐手机号：未传 phone 且有 wxPhone 时使用 wxPhone；二者皆空允许注册。
     * 微信 getPhoneNumber 非必经，用户可手填 phone 或暂不填。
     */
    private void normalizeRegisterPhone(SysEmployeeBo bo) {
        if (StringUtils.isBlank(bo.getPhone()) && StringUtils.isNotBlank(bo.getWxPhone())) {
            bo.setPhone(StringUtils.trim(bo.getWxPhone()));
        }
        if (StringUtils.isBlank(bo.getPhone())) {
            return;
        }
        String phone = StringUtils.trim(bo.getPhone());
        if (phone.length() != 11) {
            throw new ServiceException("手机号必须为11位");
        }
        bo.setPhone(phone);
    }

    /**
     * 已审核通过的微信人员不允许再次通过角色邀请码注册，避免携带旧 token 扫其它租户邀请码时被租户过滤误导。
     */
    private void ensureWxOpenidNotAlreadyApproved(String wxOpenid) {
        if (StringUtils.isBlank(wxOpenid)) {
            return;
        }
        SysEmployee employee = TenantHelper.ignore(() -> baseMapper.selectOne(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getWxOpenid, wxOpenid)
            .eq(SysEmployee::getStatus, SystemConstants.NORMAL)
            .eq(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_APPROVED)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)));
        if (ObjectUtil.isNotNull(employee)) {
            throw new ServiceException(WX_OPENID_APPROVED_MESSAGE);
        }
    }

    private boolean existsPhone(String phone, Long employeeId) {
        return baseMapper.exists(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getPhone, phone)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL)
            .ne(ObjectUtil.isNotNull(employeeId), SysEmployee::getEmployeeId, employeeId));
    }

    private static boolean isProfileEditable(String reviewStatus) {
        return StringUtils.isBlank(reviewStatus)
            || EmployeeConstants.REVIEW_APPROVED.equals(reviewStatus);
    }

    private void validateProfileEditable(SysEmployee employee) {
        if (!isProfileEditable(employee.getReviewStatus())) {
            throw new ServiceException("待审核或已拒绝人员不可编辑档案");
        }
    }

    private void validatePersonTypeChange(SysEmployee employee, String personType) {
        if (ObjectUtil.isNotNull(employee.getUserId())
            && EmployeeConstants.PERSON_TYPE_EXTERNAL.equals(personType)) {
            throw new ServiceException("内部人员不能修改为外部人员");
        }
    }

    /**
     * Hutool 身份证性别映射为系统枚举：0男 1女 2未知。
     */
    private static String mapGenderFromIdCard(String idCard) {
        int hutoolGender = IdcardUtil.getGenderByIdCard(idCard);
        if (hutoolGender == 1) {
            return "0";
        }
        if (hutoolGender == 0) {
            return "1";
        }
        return "2";
    }

    /**
     * 内部人员档案变更时同步 sys_user 基础信息。
     */
    private void syncUserProfileIfNeeded(SysEmployee employee, String name, String phone, String gender, String status) {
        if (ObjectUtil.isNull(employee.getUserId())) {
            return;
        }
        RemoteUserProfileUpdateBo command = new RemoteUserProfileUpdateBo();
        String businessId = "employee-profile:" + employee.getEmployeeId();
        command.setBusinessId(businessId);
        command.setRequestId(UUID.nameUUIDFromBytes((businessId + ':' + name + ':' + phone + ':' + gender + ':'
            + status).getBytes(StandardCharsets.UTF_8)).toString());
        command.setUserId(employee.getUserId());
        command.setNickName(name);
        command.setPhoneNumber(phone);
        command.setGender(gender);
        command.setStatus(status);
        Runnable remoteUpdate = () -> remoteUserService.updateUserProfile(command);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    remoteUpdate.run();
                }
            });
        } else {
            remoteUpdate.run();
        }
    }

    private void consumeInviteCode(Long codeId) {
        inviteCodeMapper.update(null, new LambdaUpdateWrapper<SysInviteCode>()
            .set(SysInviteCode::getUsedCount, 1)
            .set(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_DISABLED)
            .eq(SysInviteCode::getCodeId, codeId));
    }

    private void consumeRoleInviteCode(Long codeId) {
        inviteCodeMapper.update(null, new LambdaUpdateWrapper<SysInviteCode>()
            .setSql("used_count = used_count + 1")
            .eq(SysInviteCode::getCodeId, codeId));
    }

    private void applyRegisterReviewState(SysEmployee employee) {
        if (employeeAutoApprove) {
            employee.setStatus(SystemConstants.NORMAL);
            employee.setReviewStatus(EmployeeConstants.REVIEW_APPROVED);
            employee.setReviewBy(0L);
            employee.setReviewTime(new Date());
            employee.setReviewRemark("系统自动审核通过");
        } else {
            employee.setStatus(SystemConstants.DISABLE);
            employee.setReviewStatus(EmployeeConstants.REVIEW_PENDING);
        }
    }

    /**
     * 审核拒绝或待审核状态下重新注册：更新资料并清除上一轮审核痕迹。
     */
    private void updateEmployeeForReRegister(Long employeeId, SysEmployeeBo bo, SysInviteCode code) {
        LambdaUpdateWrapper<SysEmployee> wrapper = new LambdaUpdateWrapper<SysEmployee>()
            .set(SysEmployee::getName, bo.getName())
            .set(SysEmployee::getPhone, bo.getPhone())
            .set(SysEmployee::getWxPhone, bo.getWxPhone())
            .set(SysEmployee::getInviteCodeId, code.getCodeId())
            .set(SysEmployee::getGender, StringUtils.blankToDefault(bo.getGender(), "2"))
            .set(SysEmployee::getBirthDate, bo.getBirthDate())
            .set(SysEmployee::getPhoto, bo.getPhoto())
            .set(SysEmployee::getPersonType, code.getPersonType())
            .set(SysEmployee::getAppRoleCode, code.getAppRoleCode())
            .set(SysEmployee::getTenantId, code.getTenantId())
            .set(SysEmployee::getSubmitDevice, bo.getSubmitDevice())
            .eq(SysEmployee::getEmployeeId, employeeId);
        if (employeeAutoApprove) {
            wrapper.set(SysEmployee::getStatus, SystemConstants.NORMAL)
                .set(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_APPROVED)
                .set(SysEmployee::getReviewBy, 0L)
                .set(SysEmployee::getReviewTime, new Date())
                .set(SysEmployee::getReviewRemark, "系统自动审核通过");
        } else {
            wrapper.set(SysEmployee::getStatus, SystemConstants.DISABLE)
                .set(SysEmployee::getReviewStatus, EmployeeConstants.REVIEW_PENDING)
                .set(SysEmployee::getReviewBy, null)
                .set(SysEmployee::getReviewTime, null)
                .set(SysEmployee::getReviewRemark, null);
        }
        baseMapper.update(null, wrapper);
    }

    private void enrichEmployeeVos(List<SysEmployeeVo> rows) {
        enrichInviteCodes(rows);
        enrichAppRoleNames(rows);
        miniappApprovalPermissionService.enrichRoleIds(rows);
    }

    private List<Long> resolveMiniappRoleIds(Collection<Long> requestedRoleIds, Long legacyApprovalRoleId) {
        if (requestedRoleIds != null) {
            return requestedRoleIds.stream().filter(Objects::nonNull).distinct().toList();
        }
        return legacyApprovalRoleId == null ? List.of() : List.of(legacyApprovalRoleId);
    }

    private void enrichAppRoleNames(List<SysEmployeeVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        List<String> roleKeys = rows.stream()
            .map(SysEmployeeVo::getAppRoleCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(roleKeys)) {
            return;
        }
        Map<String, String> roleNameMap = employeeAppRoleService.getRoleNameMap(roleKeys);
        for (SysEmployeeVo row : rows) {
            if (StringUtils.isNotBlank(row.getAppRoleCode())) {
                row.setAppRoleName(roleNameMap.get(row.getAppRoleCode()));
            }
        }
    }

    private void enrichInviteCodes(List<SysEmployeeVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        List<Long> inviteCodeIds = rows.stream()
            .map(SysEmployeeVo::getInviteCodeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysInviteCode> inviteCodeById = CollUtil.isEmpty(inviteCodeIds) ? Map.of()
            : inviteCodeMapper.selectList(new LambdaQueryWrapper<SysInviteCode>()
                .in(SysInviteCode::getCodeId, inviteCodeIds)
                .eq(SysInviteCode::getDelFlag, SystemConstants.NORMAL))
            .stream()
            .collect(Collectors.toMap(SysInviteCode::getCodeId, code -> code, (a, b) -> a));

        List<Long> employeeIds = rows.stream()
            .map(SysEmployeeVo::getEmployeeId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, SysInviteCode> bindCodeByEmployeeId = Map.of();
        if (CollUtil.isNotEmpty(employeeIds)) {
            List<SysInviteCode> bindCodes = inviteCodeMapper.selectList(new LambdaQueryWrapper<SysInviteCode>()
                .in(SysInviteCode::getEmployeeId, employeeIds)
                .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_EMPLOYEE)
                .eq(SysInviteCode::getDelFlag, SystemConstants.NORMAL)
                .orderByDesc(SysInviteCode::getCreateTime));
            bindCodeByEmployeeId = bindCodes.stream()
                .collect(Collectors.toMap(SysInviteCode::getEmployeeId, code -> code, (a, b) -> a));
        }

        for (SysEmployeeVo row : rows) {
            SysInviteCode matched = null;
            if (ObjectUtil.isNotNull(row.getInviteCodeId())) {
                matched = inviteCodeById.get(row.getInviteCodeId());
            }
            if (ObjectUtil.isNull(matched)) {
                matched = bindCodeByEmployeeId.get(row.getEmployeeId());
            }
            if (ObjectUtil.isNotNull(matched)) {
                row.setInviteCode(matched.getInviteCode());
                row.setCodeType(matched.getCodeType());
            }
            row.setBindStatus(StringUtils.isBlank(row.getWxOpenid())
                ? EmployeeConstants.BIND_STATUS_UNBOUND : EmployeeConstants.BIND_STATUS_BOUND);
        }
    }
}
