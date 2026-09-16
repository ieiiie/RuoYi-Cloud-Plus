package com.ym.agriculture.farmtask.workorder.support;

import com.ym.common.core.utils.StringUtils;
import com.ym.agriculture.shared.i18n.BilingualContent;
import com.ym.agriculture.farmtask.i18n.StaskSmsBilingualRenderer;
import com.ym.agriculture.farmtask.employee.model.vo.SysEmployeeVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * stask 任务短信发送（通用模板 {@code ${content}请登录小程序查看详情。}）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sms", name = "enabled", havingValue = "true")
public class SfStaskSmsNotifySender {

    private static final String SMS_BLEND = "alert";
    private static final String PARAM_CONTENT = "content";

    private final SfStaskEmployeeAccessor employeeAccessor;
    private final StaskSmsBilingualRenderer contentRenderer;

    /**
     * 阿里云短信模板 CODE，审核通过后配置 {@code ym.stask.notify.sms-template-id}。
     */
    @Value("${ym.stask.notify.sms-template-id:}")
    private String smsTemplateId;

    /**
     * 同步发送农事任务通知短信。
     *
     * @param scene    通知场景（仅日志）
     * @param targetId 通知对象员工ID
     * @param content  中维双语通知正文
     * @return 是否发送成功
     */
    public boolean send(String scene, Long targetId, BilingualContent content) {
        if (StringUtils.isBlank(smsTemplateId)) {
            log.warn("stask短信未配置模板 CODE，跳过发送 scene={}, targetId={}", scene, targetId);
            return false;
        }
        if (content == null || StringUtils.isBlank(content.zhCn()) || StringUtils.isBlank(content.ugCn())) {
            log.warn("stask短信跳过：双语正文不完整 scene={}, targetId={}", scene, targetId);
            return false;
        }
        if (targetId == null) {
            log.warn("stask短信跳过：targetId 为空 scene={}", scene);
            return false;
        }
        String phone = resolvePhone(targetId);
        if (StringUtils.isBlank(phone)) {
            log.warn("stask短信跳过：员工无可用手机号 scene={}, targetId={}", scene, targetId);
            return false;
        }
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put(PARAM_CONTENT, contentRenderer.render(content));
        try {
            SmsBlend sms = SmsFactory.getSmsBlend(SMS_BLEND);
            SmsResponse resp = sms.sendMessage(phone, smsTemplateId, params);
            if (resp != null && resp.isSuccess()) {
                log.info("stask短信已发送 scene={}, targetId={}, phone={}", scene, targetId, maskPhone(phone));
                return true;
            }
            log.warn("stask短信发送失败 scene={}, targetId={}, phone={}", scene, targetId, maskPhone(phone));
            return false;
        } catch (Exception e) {
            log.warn("stask短信发送异常 scene={}, targetId={}, phone={}, errorType={}",
                scene, targetId, maskPhone(phone), e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * 异步发送农事任务通知短信。
     *
     * @param scene    通知场景（仅日志）
     * @param targetId 通知对象员工ID
     * @param content  中维双语通知正文
     */
    @Async("smsNotificationExecutor")
    public void sendAsync(String scene, Long targetId, BilingualContent content) {
        send(scene, targetId, content);
    }

    /**
     * 按员工ID解析手机号：phone 优先，wx_phone 兜底。
     */
    private String resolvePhone(Long employeeId) {
        List<SysEmployeeVo> employees = employeeAccessor.queryByIds(List.of(employeeId));
        if (employees == null || employees.isEmpty()) {
            return null;
        }
        SysEmployeeVo employee = employees.get(0);
        if (StringUtils.isNotBlank(employee.getPhone())) {
            return employee.getPhone().trim();
        }
        if (StringUtils.isNotBlank(employee.getWxPhone())) {
            return employee.getWxPhone().trim();
        }
        return null;
    }

    static String maskPhone(String phone) {
        if (phone == null || phone.length() <= 7) {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
