SET NAMES utf8mb4;

-- YM 农业与 IoT 生产周期任务（Snail Job Server 2.0.2）。
-- 可重复执行：biz_id 是稳定业务键，同执行器的旧重复任务仅关闭不删除。
-- 部署顺序：先上线执行器并确认 sj_job_executor 注册，再执行本脚本。

USE `snail_job`;

DROP TEMPORARY TABLE IF EXISTS `tmp_ym_sj_job`;
CREATE TEMPORARY TABLE `tmp_ym_sj_job` (
    `namespace_id` varchar(64) NOT NULL,
    `biz_id` varchar(64) NOT NULL,
    `group_name` varchar(64) NOT NULL,
    `job_name` varchar(64) NOT NULL,
    `job_status` tinyint NOT NULL,
    `task_type` tinyint NOT NULL,
    `executor_info` varchar(255) NOT NULL,
    `trigger_type` tinyint NOT NULL,
    `trigger_interval` varchar(255) NOT NULL,
    `executor_timeout` int NOT NULL,
    `max_retry_times` int NOT NULL,
    `retry_interval` int NOT NULL,
    `description` varchar(256) NOT NULL,
    PRIMARY KEY (`namespace_id`, `biz_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO `tmp_ym_sj_job`
(`namespace_id`, `biz_id`, `group_name`, `job_name`, `job_status`, `task_type`, `executor_info`,
 `trigger_type`, `trigger_interval`, `executor_timeout`, `max_retry_times`, `retry_interval`, `description`)
VALUES
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.i18n-text', 'ym-agriculture', '业务文本翻译补偿', 1, 1, 'sfI18nTextProcessJob', 2, '30', 300, 1, 30, '全局翻译队列扫描'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.news-ingest', 'ym-agriculture', '农业资讯暂存处理', 1, 1, 'sfNewsIngestJob', 2, '10', 300, 1, 30, '全局资讯暂存队列扫描'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.news-media', 'ym-agriculture', '农业资讯媒体转存', 1, 1, 'sfNewsMediaTransferJob', 2, '10', 900, 1, 60, '全局媒体转存队列补偿扫描'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.voice-broadcast', 'ym-agriculture', '工单语音播报生成', 1, 1, 'sfStaskVoiceBroadcastJob', 2, '30', 600, 1, 60, '全局语音播报队列扫描'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.alg-token', 'ym-agriculture', '算法中台Token刷新', 1, 2, 'algBackTokenRefreshJob', 2, '1500', 120, 1, 60, '广播刷新每个节点的本地Token'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.inspection-ai-recovery', 'ym-agriculture', '巡查AI超时恢复', 1, 1, 'sfInspectionAiRecoveryJob', 2, '60', 300, 2, 60, '逐有效租户标记超时生成任务'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.market-ingest', 'ym-agriculture', '农业行情暂存处理', 1, 1, 'sfMarketQuoteIngestJob', 2, '60', 300, 1, 60, '全局行情暂存队列扫描'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.solar-term', 'ym-agriculture', '节气数据确保', 1, 1, 'sfSolarTermEnsureJob', 3, '0 10 0 * * ?', 300, 2, 60, '每日00:10逐有效租户确保节气数据'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.weather-forecast', 'ym-agriculture', '天气预报同步', 1, 1, 'sfWeatherForecastSyncJob', 2, '10800', 1800, 2, 300, '每3小时逐有效租户同步天气预报'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.weather-alert', 'ym-agriculture', '气象预警同步', 1, 1, 'sfWeatherAlertSyncJob', 2, '600', 1800, 2, 120, '全局同步启用省份气象预警'),
('764d604ec6fc45f68cd92514c40e9e1a', 'ym-agriculture.satellite-image-migrate', 'ym-agriculture', '遥感影像历史迁移', 0, 1, 'sfSatelliteImageMigrateJob', 2, '86400', 3600, 0, 0, '仅供手工触发，禁止自动运行');

-- 先关闭同分组、同执行器但 biz_id 非规范值的历史重复任务。
UPDATE `sj_job` old_job
JOIN `tmp_ym_sj_job` canonical
  ON canonical.`namespace_id` = old_job.`namespace_id`
 AND canonical.`group_name` = old_job.`group_name`
 AND canonical.`executor_info` = old_job.`executor_info`
SET old_job.`job_status` = 0,
    old_job.`update_dt` = NOW()
WHERE old_job.`deleted` = 0
  AND old_job.`biz_id` <> canonical.`biz_id`;

INSERT INTO `sj_job`
(`namespace_id`, `biz_id`, `group_name`, `job_name`, `args_str`, `args_type`, `next_trigger_at`,
 `job_status`, `task_type`, `route_key`, `executor_type`, `executor_info`, `trigger_type`, `trigger_interval`,
 `block_strategy`, `executor_timeout`, `max_retry_times`, `parallel_num`, `retry_interval`, `bucket_index`,
 `resident`, `notify_ids`, `labels`, `description`, `ext_attrs`, `deleted`)
SELECT `namespace_id`, `biz_id`, `group_name`, `job_name`, NULL, 1,
       CASE WHEN `job_status` = 1 THEN CAST(UNIX_TIMESTAMP(CURRENT_TIMESTAMP(3)) * 1000 AS UNSIGNED) ELSE 0 END,
       `job_status`, `task_type`, 4, 1, `executor_info`, `trigger_type`, `trigger_interval`,
       1, `executor_timeout`, `max_retry_times`, 1, `retry_interval`, 0,
       0, '', '', `description`, '', 0
FROM `tmp_ym_sj_job`
ON DUPLICATE KEY UPDATE
`group_name` = VALUES(`group_name`),
`job_name` = VALUES(`job_name`),
`args_str` = VALUES(`args_str`),
`args_type` = VALUES(`args_type`),
`next_trigger_at` = VALUES(`next_trigger_at`),
`job_status` = VALUES(`job_status`),
`task_type` = VALUES(`task_type`),
`route_key` = VALUES(`route_key`),
`executor_type` = VALUES(`executor_type`),
`executor_info` = VALUES(`executor_info`),
`trigger_type` = VALUES(`trigger_type`),
`trigger_interval` = VALUES(`trigger_interval`),
`block_strategy` = VALUES(`block_strategy`),
`executor_timeout` = VALUES(`executor_timeout`),
`max_retry_times` = VALUES(`max_retry_times`),
`parallel_num` = VALUES(`parallel_num`),
`retry_interval` = VALUES(`retry_interval`),
`resident` = VALUES(`resident`),
`description` = VALUES(`description`),
`deleted` = 0,
`update_dt` = NOW();

-- 验收期望：agriculture total=14/enabled=13，iot total=7/enabled=7。
SELECT `group_name`, COUNT(*) AS total,
       SUM(`job_status` = 1) AS enabled,
       SUM(`task_type` = 2) AS broadcast
FROM `sj_job`
WHERE `namespace_id` = '764d604ec6fc45f68cd92514c40e9e1a'
  AND `biz_id` IN (SELECT `biz_id` FROM `tmp_ym_sj_job`)
  AND `deleted` = 0
GROUP BY `group_name`
ORDER BY `group_name`;

DROP TEMPORARY TABLE IF EXISTS `tmp_ym_sj_job`;
