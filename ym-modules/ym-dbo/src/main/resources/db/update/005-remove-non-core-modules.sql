-- Dbo 运营平台核心化清理（MySQL）。仅在独立数据库 ry_dbo 中执行。
-- 移除工作流、任务调度、代码生成、AI、演示、第三方登录和外置监控扩展。

DELETE rm
FROM sys_role_menu rm
JOIN sys_menu m ON m.menu_id = rm.menu_id
WHERE m.menu_id IN (1761400000000000003, 1761400000000000004, 1761400000000000005, 1761400000000000008,
                    1761400000000000115, 1761400000000000116, 1761400000000000117, 1761400000000000120,
                    1761400000000000121, 1761400000000011616, 1761400000000011618)
   OR m.component LIKE 'tool/gen%'
   OR m.component LIKE 'demo/%'
   OR m.component LIKE 'ai/%'
   OR m.component LIKE 'workflow/%'
   OR m.perms LIKE 'tool:gen:%'
   OR m.perms LIKE 'demo:%'
   OR m.perms LIKE 'workflow:%';

DELETE FROM sys_menu
WHERE menu_id IN (1761400000000000003, 1761400000000000004, 1761400000000000005, 1761400000000000008,
                  1761400000000000115, 1761400000000000116, 1761400000000000117, 1761400000000000120,
                  1761400000000000121, 1761400000000011616, 1761400000000011618)
   OR component LIKE 'tool/gen%'
   OR component LIKE 'demo/%'
   OR component LIKE 'ai/%'
   OR component LIKE 'workflow/%'
   OR perms LIKE 'tool:gen:%'
   OR perms LIKE 'demo:%'
   OR perms LIKE 'workflow:%';

DELETE FROM sys_user_post WHERE user_id IN (1761100000000000003, 1761100000000000004);
DELETE FROM sys_user_role WHERE user_id IN (1761100000000000003, 1761100000000000004);
DELETE FROM sys_user WHERE user_id IN (1761100000000000003, 1761100000000000004);
DELETE FROM sys_role WHERE role_id IN (1761300000000000003, 1761300000000000004);

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS test_leave, test_tree, test_demo, gen_table_column, gen_table;
DROP TABLE IF EXISTS flow_instance_biz_ext, flow_spel, flow_category, flow_user, flow_his_task, flow_task,
                     flow_instance, flow_skip, flow_node, flow_definition;
DROP TABLE IF EXISTS sai_agent_skill, sai_skill_file, sai_skill, sai_agent_mcp_server, sai_mcp_server,
                     sai_agent_conversation_record, sai_agent_conversation, sai_agent_usage_stat, sai_user_agent,
                     sai_agent, sai_rag_document_image, sai_rag_chunk, sai_rag_document, sai_rag,
                     sai_model_usage_stat, sai_model_config, sai_model_provider, sai_openapi_user, sai_user,
                     sai_client_node, sai_store_instance, sai_resource, sai_app;
DROP TABLE IF EXISTS sj_workflow_task_batch, sj_workflow_node, sj_workflow, sj_retry_summary, sj_job_summary,
                     sj_job_task_batch, sj_job_task, sj_job_log_message, sj_job_executor, sj_job,
                     sj_system_user_permission, sj_system_user, sj_distributed_lock, sj_server_node,
                     sj_retry_scene_config, sj_retry_task_log_message, sj_retry_task, sj_retry,
                     sj_retry_dead_letter, sj_notify_recipient, sj_notify_config, sj_group_config, sj_namespace;
SET FOREIGN_KEY_CHECKS = 1;
