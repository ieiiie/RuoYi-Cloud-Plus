#!/usr/bin/env bash
set -euo pipefail

repo_dir=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "${repo_dir}"

scopes=(
  ym-api/ym-api-agriculture
  ym-api/ym-api-iot
  ym-modules/ym-agriculture
  ym-modules/ym-iot
  ym-modules/ym-agriculture-miniapp
  ym-modules/ym-farm-task-miniapp
)

failures=0

reject_matches() {
  local description=$1
  shift
  local output
  output=$("$@" || true)
  if [[ -n "${output}" ]]; then
    echo "[FAIL] ${description}"
    echo "${output}"
    failures=$((failures + 1))
  else
    echo "[PASS] ${description}"
  fi
}

reject_matches "无 TableDataInfo、废弃 materialinventory、@Scheduled 或 Redis 延迟队列入口" \
  rg -n 'TableDataInfo|materialinventory|@Scheduled|RedisDelayQueue' "${scopes[@]}"

reject_matches "Controller 分页接口不直接返回 PageResult/IPage/Page" \
  rg -n --glob '*Controller.java' \
    'public[[:space:]]+(PageResult|IPage|Page)<' "${scopes[@]}"

reject_matches "农业服务不直接依赖 IoT Entity/Mapper/Service" \
  bash -c "rg -n '^import com\\.ym\\.iot\\.' ym-modules/ym-agriculture/src/main/java | rg -v 'import com\\.ym\\.iot\\.api\\.'"

reject_matches "IoT 服务不直接依赖农业 Entity/Mapper/Service" \
  bash -c "rg -n '^import com\\.ym\\.agriculture\\.' ym-modules/ym-iot/src/main/java | rg -v 'import com\\.ym\\.agriculture\\.api\\.'"

reject_matches "API 模块不包含 Entity、Mapper 或 Service 实现" \
  rg -n '(^|/)(entity|mapper|service/impl)(/|$)|@TableName|BaseMapper<' ym-api/ym-api-agriculture ym-api/ym-api-iot

reject_matches "领域 Mapper XML 不包含跨库限定写入或 JOIN" \
  rg -n '(?i)(join|update|insert[[:space:]]+into|delete[[:space:]]+from)[[:space:]]+`?(ry-cloud|ym-agriculture|ym-iot)\.' \
    ym-modules/ym-agriculture/src/main/resources ym-modules/ym-iot/src/main/resources

if (( failures > 0 )); then
  echo "边界检查失败项: ${failures}"
  exit 1
fi

python3 script/verify-agriculture-domains.py

echo "迁移架构边界检查全部通过"
