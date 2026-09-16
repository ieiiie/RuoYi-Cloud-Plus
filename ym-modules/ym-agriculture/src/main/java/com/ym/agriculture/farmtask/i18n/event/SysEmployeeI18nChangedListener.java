package com.ym.agriculture.farmtask.i18n.event;

import com.ym.agriculture.shared.i18n.model.I18nTextSource;
import com.ym.agriculture.farmtask.employee.event.SysEmployeeTextChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 将系统库人员姓名变化最终一致地登记到智慧农业翻译表。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysEmployeeI18nChangedListener {

    private final SysEmployeeI18nSourceReader sourceReader;
    private final SfEmployeeI18nRegistrationExecutor registrationExecutor;

    /**
     * 消费已提交的人员姓名变化事件。
     *
     * @param event 人员姓名变化事件
     */
    @EventListener
    public void onEmployeeTextChanged(SysEmployeeTextChangedEvent event) {
        if (event == null) {
            return;
        }
        try {
            I18nTextSource source = sourceReader.read(event);
            if (source != null) {
                registrationExecutor.register(event.tenantId(), source);
            }
        } catch (Exception e) {
            log.warn("人员翻译资源登记失败 tenantId={}, employeeId={}, errorType={}",
                event.tenantId(), event.employeeId(), e.getClass().getSimpleName());
        }
    }
}
