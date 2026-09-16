package com.ym.agriculture.farmtask.workorder.support;

import com.ym.agriculture.shared.i18n.BilingualContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * stask 任务通知扩展点（默认仅记录日志，不发短信）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SfStaskNotifyService {

    private final StaskNotificationPublisher notificationPublisher;

    /**
     * 发布通知事件；实际交付由监听器在事务提交后调用。
     *
     * @param scene    通知场景
     * @param targetId 通知对象员工ID
     * @param content  双语通知内容；缺省实现不持久化也不记录正文
     */
    public void notify(String scene, Long targetId, BilingualContent content) {
        notificationPublisher.publish(scene, targetId, content);
    }

    /**
     * 通知交付扩展点。缺省实现仅记录元数据，不建设收件箱、SSE 或正文持久化。
     */
    public void deliver(String scene, Long targetId, BilingualContent content) {
        log.info("stask任务通知已触发 scene={}, targetId={}, bilingual={}", scene, targetId, content != null);
    }
}
