package com.ym.agriculture.farmtask.employee.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ym.common.core.constant.SystemConstants;
import com.ym.common.core.exception.ServiceException;
import com.ym.common.core.utils.MapstructUtils;
import com.ym.common.core.utils.StringUtils;
import com.ym.common.mybatis.core.page.PageQuery;
import com.ym.common.core.domain.PageResult;
import com.ym.common.redis.utils.RedisUtils;
import com.ym.common.tenant.helper.TenantHelper;
import com.ym.agriculture.farmtask.employee.constant.EmployeeConstants;
import com.ym.agriculture.farmtask.employee.dao.SysEmployeeMapper;
import com.ym.agriculture.farmtask.employee.dao.SysInviteCodeMapper;
import com.ym.agriculture.farmtask.employee.model.bo.SysInviteCodeBo;
import com.ym.agriculture.farmtask.employee.model.entity.SysEmployee;
import com.ym.agriculture.farmtask.employee.model.entity.SysInviteCode;
import com.ym.agriculture.farmtask.employee.service.IEmployeeAppRoleService;
import com.ym.agriculture.farmtask.employee.model.vo.InviteVerifyVo;
import com.ym.agriculture.farmtask.employee.model.vo.MiniProgramCodeVo;
import com.ym.agriculture.farmtask.employee.model.vo.SysInviteCodeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.ym.common.core.constant.GlobalConstants.GLOBAL_REDIS_KEY;

/**
 * 邀请码服务实现。
 */
@RequiredArgsConstructor
@Service
public class SysInviteCodeServiceImpl implements com.ym.agriculture.farmtask.employee.service.ISysInviteCodeService {

    private static final int MAX_RETRY_COUNT = 3;

    private static final int DAILY_ROLE_INVITE_LIMIT = 100;

    private static final String LIST_STATUS_ACTIVE = "active";

    private static final String LIST_STATUS_EXPIRED = "expired";

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=%s&secret=%s";

    private static final String WXACODE_URL = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=%s";

    private static final String ACCESS_TOKEN_CACHE_KEY_PREFIX = GLOBAL_REDIS_KEY+"wechat:miniprogram:access_token:";

    private static final String MINI_CODE_CACHE_KEY_PREFIX = GLOBAL_REDIS_KEY+"wechat:miniprogram:invite_code_qr:";

    private static final long ACCESS_TOKEN_REFRESH_ADVANCE_SECONDS = 300L;

    private final SysInviteCodeMapper baseMapper;

    private final SysEmployeeMapper employeeMapper;

    private final IEmployeeAppRoleService employeeAppRoleService;

    @Value("${wechat.employee-miniprogram.app-id:}")
    private String appId;

    @Value("${wechat.employee-miniprogram.app-secret:}")
    private String appSecret;

    @Value("${wechat.employee-miniprogram.invite-code.page:pages/register/index}")
    private String defaultMiniCodePage;

    @Value("${wechat.employee-miniprogram.invite-code.env-version:release}")
    private String defaultEnvVersion;

    @Override
    public PageResult<SysInviteCodeVo> queryPage(SysInviteCodeBo bo, PageQuery pageQuery) {
        Page<SysInviteCodeVo> page = baseMapper.selectPageInviteCodeList(pageQuery.build(), buildQueryWrapper(bo));
        enrichAppRoleNames(page.getRecords());
        return com.ym.agriculture.shared.common.AgriculturePageResults.build(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysInviteCodeVo generateRoleInviteCode(SysInviteCodeBo bo) {
        validateRoleInviteDate(bo);
        employeeAppRoleService.requireInviteSelectableRoleCode(bo.getAppRoleCode());
        long todayCount = baseMapper.selectCount(new LambdaQueryWrapper<SysInviteCode>()
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_ROLE)
            .between(SysInviteCode::getCreateTime, DateUtil.beginOfDay(new Date()), DateUtil.endOfDay(new Date())));
        if (todayCount >= DAILY_ROLE_INVITE_LIMIT) {
            throw new ServiceException("今日邀请码生成数量已达上限");
        }

        SysInviteCode code = new SysInviteCode();
        code.setInviteCode(generateUniqueCode(6));
        code.setCodeType(EmployeeConstants.INVITE_CODE_TYPE_ROLE);
        code.setAppRoleCode(bo.getAppRoleCode());
        code.setPersonType(StringUtils.blankToDefault(bo.getPersonType(), EmployeeConstants.PERSON_TYPE_EXTERNAL));
        code.setMaxUses(ObjectUtil.defaultIfNull(bo.getMaxUses(), 9999));
        code.setUsedCount(0);
        code.setExpireTime(bo.getExpireTime());
        code.setStatus(EmployeeConstants.INVITE_STATUS_VALID);
        code.setDelFlag(SystemConstants.NORMAL);
        code.setRemark(bo.getRemark());
        baseMapper.insert(code);
        SysInviteCodeVo vo = baseMapper.selectVoById(code.getCodeId());
        enrichAppRoleNames(List.of(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysInviteCodeVo generateEmployeeBindCode(Long employeeId) {
        if (ObjectUtil.isNull(employeeId)) {
            throw new ServiceException("员工ID不能为空");
        }
        SysInviteCode code = new SysInviteCode();
        code.setInviteCode(generateUniqueCode(4));
        code.setCodeType(EmployeeConstants.INVITE_CODE_TYPE_EMPLOYEE);
        code.setEmployeeId(employeeId);
        code.setPersonType(EmployeeConstants.PERSON_TYPE_EXTERNAL);
        code.setMaxUses(1);
        code.setUsedCount(0);
        code.setStatus(EmployeeConstants.INVITE_STATUS_VALID);
        code.setDelFlag(SystemConstants.NORMAL);
        baseMapper.insert(code);
        return baseMapper.selectVoById(code.getCodeId());
    }

    @Override
    public InviteVerifyVo verifyRoleInviteCode(String inviteCode) {
        return verifyRoleInviteCode(inviteCode, null);
    }

    @Override
    public InviteVerifyVo verifyRoleInviteCode(String inviteCode, String wxOpenid) {
        SysInviteCode code = requireValidRoleInviteCode(inviteCode, wxOpenid);
        InviteVerifyVo vo = new InviteVerifyVo();
        vo.setCodeId(code.getCodeId());
        vo.setInviteCode(code.getInviteCode());
        vo.setAppRoleCode(code.getAppRoleCode());
        vo.setAppRoleName(resolveRoleName(code.getAppRoleCode()));
        vo.setPersonType(code.getPersonType());
        vo.setValid(Boolean.TRUE);
        return vo;
    }

    @Override
    public SysInviteCodeVo queryById(Long codeId) {
        SysInviteCodeVo vo = baseMapper.selectVoById(codeId);
        if (ObjectUtil.isNotNull(vo)) {
            enrichAppRoleNames(List.of(vo));
        }
        return vo;
    }

    @Override
    public SysInviteCode requireValidRoleInviteCode(String inviteCode) {
        return requireValidRoleInviteCode(inviteCode, null);
    }

    @Override
    public SysInviteCode requireValidRoleInviteCode(String inviteCode, String wxOpenid) {
        if (StringUtils.isBlank(inviteCode) || inviteCode.length() != 6) {
            throw new ServiceException("请输入6位数字邀请码");
        }
        SysInviteCode code = baseMapper.selectOne(new LambdaQueryWrapper<SysInviteCode>()
            .eq(SysInviteCode::getInviteCode, inviteCode)
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_ROLE)
            .eq(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_VALID)
            .eq(SysInviteCode::getDelFlag, SystemConstants.NORMAL));
        if (ObjectUtil.isNull(code)) {
            throw new ServiceException("邀请码不存在或已失效");
        }
        employeeAppRoleService.requireInviteSelectableRoleCode(code.getAppRoleCode());
        if (ObjectUtil.isNotNull(code.getExpireTime()) && !code.getExpireTime().after(new Date())) {
            throw new ServiceException("邀请码不存在或已过期");
        }
        if (isInviteCodeExhausted(code) && !isReturningInviteUser(wxOpenid, code.getCodeId())) {
            throw new ServiceException("该邀请码已达到最大使用次数");
        }
        return code;
    }

    /**
     * 邀请码使用次数是否已用尽。
     */
    private boolean isInviteCodeExhausted(SysInviteCode code) {
        return ObjectUtil.defaultIfNull(code.getUsedCount(), 0) >= ObjectUtil.defaultIfNull(code.getMaxUses(), 1);
    }

    /**
     * 当前微信是否已使用该邀请码提交过人员注册（含待审核、已通过、已拒绝）。
     */
    private boolean isReturningInviteUser(String wxOpenid, Long inviteCodeId) {
        if (StringUtils.isBlank(wxOpenid) || ObjectUtil.isNull(inviteCodeId)) {
            return false;
        }
        return employeeMapper.exists(new LambdaQueryWrapper<SysEmployee>()
            .eq(SysEmployee::getWxOpenid, wxOpenid)
            .eq(SysEmployee::getInviteCodeId, inviteCodeId)
            .eq(SysEmployee::getDelFlag, SystemConstants.NORMAL));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteInviteCode(Long codeId) {
        return baseMapper.update(null, new LambdaUpdateWrapper<SysInviteCode>()
            .set(SysInviteCode::getDelFlag, SystemConstants.DISABLE)
            .eq(SysInviteCode::getCodeId, codeId)
            .eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_ROLE));
    }

    @Override
    public MiniProgramCodeVo generateMiniProgramCode(Long codeId, SysInviteCodeBo bo) {
        SysInviteCodeVo code = queryById(codeId);
        if (ObjectUtil.isNull(code)) {
            throw new ServiceException("邀请码不存在");
        }
        if (!EmployeeConstants.INVITE_STATUS_VALID.equals(code.getStatus())) {
            throw new ServiceException("邀请码已停用，无法生成小程序码");
        }
        if (ObjectUtil.isNotNull(code.getExpireTime()) && !code.getExpireTime().after(new Date())) {
            throw new ServiceException("邀请码已过期，无法生成小程序码");
        }
        String page = StringUtils.blankToDefault(bo != null ? bo.getPage() : null, defaultMiniCodePage);
        String envVersion = StringUtils.blankToDefault(bo != null ? bo.getEnvVersion() : null, defaultEnvVersion);
        String scene = buildMiniProgramScene(code);
        String cacheKey = buildMiniCodeCacheKey(code, page, envVersion);
        MiniProgramCodeVo cached = TenantHelper.ignore(() -> RedisUtils.getCacheObject(cacheKey));
        if (ObjectUtil.isNotNull(cached)) {
            return cached;
        }

        MiniProgramCodeVo vo = new MiniProgramCodeVo();
        vo.setCodeId(code.getCodeId());
        vo.setTenantId(code.getTenantId());
        vo.setInviteCode(code.getInviteCode());
        vo.setPage(page);
        vo.setScene(scene);
        vo.setEnvVersion(envVersion);
        vo.setImageBase64(Base64.getEncoder().encodeToString(requestMiniProgramCode(scene, page, envVersion)));
        cacheMiniProgramCode(cacheKey, vo, code.getExpireTime());
        return vo;
    }

    private LambdaQueryWrapper<SysInviteCode> buildQueryWrapper(SysInviteCodeBo bo) {
        LambdaQueryWrapper<SysInviteCode> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysInviteCode::getCodeType, EmployeeConstants.INVITE_CODE_TYPE_ROLE)
            .eq(SysInviteCode::getDelFlag, SystemConstants.NORMAL)
            .like(StringUtils.isNotBlank(bo.getKeyword()), SysInviteCode::getInviteCode, bo.getKeyword())
            .eq(StringUtils.isNotBlank(bo.getStatus()), SysInviteCode::getStatus, bo.getStatus())
            .eq(StringUtils.isNotBlank(bo.getAppRoleCode()), SysInviteCode::getAppRoleCode, bo.getAppRoleCode())
            .orderByDesc(SysInviteCode::getCreateTime);
        Date now = new Date();
        if (LIST_STATUS_ACTIVE.equals(bo.getListStatus())) {
            wrapper.eq(SysInviteCode::getStatus, EmployeeConstants.INVITE_STATUS_VALID)
                .and(w -> w.isNull(SysInviteCode::getExpireTime).or().gt(SysInviteCode::getExpireTime, now));
        } else if (LIST_STATUS_EXPIRED.equals(bo.getListStatus())) {
            wrapper.isNotNull(SysInviteCode::getExpireTime).le(SysInviteCode::getExpireTime, now);
        }
        return wrapper;
    }

    private void enrichAppRoleNames(List<SysInviteCodeVo> rows) {
        if (CollUtil.isEmpty(rows)) {
            return;
        }
        List<String> roleKeys = rows.stream()
            .map(SysInviteCodeVo::getAppRoleCode)
            .filter(StringUtils::isNotBlank)
            .distinct()
            .toList();
        if (CollUtil.isEmpty(roleKeys)) {
            return;
        }
        Map<String, String> roleNameMap = employeeAppRoleService.getRoleNameMap(roleKeys);
        for (SysInviteCodeVo row : rows) {
            if (StringUtils.isNotBlank(row.getAppRoleCode())) {
                row.setAppRoleName(roleNameMap.get(row.getAppRoleCode()));
            }
        }
    }

    private String resolveRoleName(String roleKey) {
        return employeeAppRoleService.getRoleName(roleKey);
    }

    private void validateRoleInviteDate(SysInviteCodeBo bo) {
        if (ObjectUtil.isNull(bo.getExpireTime())) {
            return;
        }
        if (!bo.getExpireTime().after(new Date())) {
            throw new ServiceException("过期时间必须晚于当前时间");
        }
    }

    private String generateUniqueCode(int length) {
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            String code = length == 4
                ? String.valueOf(RandomUtil.randomInt(1000, 10000))
                : String.valueOf(RandomUtil.randomInt(100000, 1000000));
            boolean exists = TenantHelper.ignore(() -> baseMapper.exists(new LambdaQueryWrapper<SysInviteCode>()
                .eq(SysInviteCode::getInviteCode, code)));
            if (!exists) {
                return code;
            }
        }
        throw new ServiceException("邀请码生成失败，请重试");
    }

    private byte[] requestMiniProgramCode(String scene, String page, String envVersion) {
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(appSecret)) {
            throw new ServiceException("未配置微信小程序 app-id 或 app-secret，无法生成小程序码");
        }
        String accessToken = getCachedAccessToken();
        JSONObject body = new JSONObject();
        body.put("scene", scene);
        body.put("page", page);
        body.put("env_version", envVersion);
        body.put("check_path", false);
        byte[] bytes = HttpRequest.post(String.format(WXACODE_URL, accessToken))
            .body(body.toJSONString())
            .timeout(15000)
            .execute()
            .bodyBytes();
        if (bytes.length > 0 && bytes[0] == '{') {
            JSONObject err = JSON.parseObject(new String(bytes));
            throw new ServiceException("生成小程序码失败：" + err.getString("errmsg"));
        }
        return bytes;
    }

    private String buildMiniProgramScene(SysInviteCodeVo code) {
        String tenantId = StringUtils.blankToDefault(code.getTenantId(), TenantHelper.getTenantId());
        String scene = "i:" + code.getInviteCode() + ",t:" + tenantId;
        if (scene.length() > 32) {
            throw new ServiceException("小程序码参数过长，请检查邀请码和租户编号长度");
        }
        return scene;
    }

    private String buildMiniCodeCacheKey(SysInviteCodeVo code, String page, String envVersion) {
        return  MINI_CODE_CACHE_KEY_PREFIX
            + StringUtils.blankToDefault(code.getTenantId(), TenantHelper.getTenantId())
            + ":" + code.getInviteCode()
            + ":" + page
            + ":" + envVersion;
    }

    private void cacheMiniProgramCode(String cacheKey, MiniProgramCodeVo vo, Date expireTime) {
        if (ObjectUtil.isNull(expireTime)) {
            return;
        }
        long ttlMillis = expireTime.getTime() - System.currentTimeMillis();
        if (ttlMillis <= 0) {
            return;
        }
        TenantHelper.ignore(() -> {
            RedisUtils.setCacheObject(cacheKey, vo, Duration.ofMillis(ttlMillis));
            return null;
        });
    }

    private String getCachedAccessToken() {
        String cacheKey = ACCESS_TOKEN_CACHE_KEY_PREFIX + appId;
        String cachedToken = TenantHelper.ignore(() -> RedisUtils.getCacheObject(cacheKey));
        if (StringUtils.isNotBlank(cachedToken)) {
            return cachedToken;
        }

        String tokenResp = HttpUtil.get(String.format(ACCESS_TOKEN_URL, appId, appSecret), 15000);
        JSONObject tokenJson = JSON.parseObject(tokenResp);
        String accessToken = tokenJson.getString("access_token");
        if (StringUtils.isBlank(accessToken)) {
            throw new ServiceException("获取微信 access_token 失败：" + tokenJson.getString("errmsg"));
        }

        long expiresIn = tokenJson.getLongValue("expires_in");
        long cacheSeconds = Math.max(60L, expiresIn - ACCESS_TOKEN_REFRESH_ADVANCE_SECONDS);
        TenantHelper.ignore(() -> {
            RedisUtils.setCacheObject(cacheKey, accessToken, Duration.ofSeconds(cacheSeconds));
            return null;
        });
        return accessToken;
    }
}
