#!/usr/bin/env bash
set -euo pipefail

# 业务字典数据在数据库迁移完成后，旧的空结果仍可能留在 Redis hash 中。
# 只删除农业和 IoT 已知字典字段，不删除 hash，不影响系统通用字典。
redis_container="${REDIS_CONTAINER:-ym-redis}"
redis_database="${REDIS_DATABASE:-0}"
dict_cache_key="${DICT_CACHE_KEY:-global:sys_dict}"

business_dicts=(
  ai_model
  flight_task_status
  iot_alert_level
  iot_alert_type
  iot_data_format
  iot_device_status
  iot_device_type
  iot_handle_status
  iot_log_type
  iot_net_type
  iot_node_type
  iot_online_status
  iot_product_status
  iot_protocol
  iot_rule_type
  remote_sensing_croptype
  sat_task_type
  waylines_type
)

deleted_count="$(
  docker exec "${redis_container}" redis-cli -n "${redis_database}" \
    HDEL "${dict_cache_key}" "${business_dicts[@]}"
)"

printf 'business dictionary cache fields removed: %s\n' "${deleted_count}"
printf 'cache key: %s, redis database: %s\n' "${dict_cache_key}" "${redis_database}"
