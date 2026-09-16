#!/usr/bin/env bash
# 仅启动已确认的本地 YM 服务，不运行参考 Compose 中的整套中间件。
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
infra_compose=/media/admin/extra/docker-services/ym-infra/compose.yaml
compose=(docker compose --env-file "$script_dir/.env" -f "$script_dir/docker-compose.yml")

docker info >/dev/null
"${compose[@]}" config --quiet
docker compose -f "$infra_compose" config --quiet

# 使用既有项目名和命名数据卷；不要使用 down -v。
docker compose -f "$infra_compose" up -d --no-deps --wait --wait-timeout 180 mysql redis

# Nacos 是现存独立容器，不创建同名替代品，不迁移其数据。
image="$(docker inspect -f '{{.Config.Image}}' ruoyi-nacos-local)"
if [[ "$image" != nacos/nacos-server:* ]]; then
  printf 'Unexpected Nacos image: %s\n' "$image" >&2
  exit 1
fi
docker update --memory 1536m --memory-reservation 768m --memory-swap 1792m ruoyi-nacos-local
docker start ruoyi-nacos-local >/dev/null
ready=false
for ((attempt=0; attempt<60; attempt++)); do
  if curl --fail --silent --max-time 3 http://127.0.0.1:8848/nacos/v3/admin/core/state/readiness >/dev/null; then
    ready=true
    break
  fi
  sleep 2
done
if [[ "$ready" != true ]]; then
  printf 'Nacos readiness failed; YM services were not started.\n' >&2
  exit 1
fi

# 依次等待 HTTP 健康，避免 JVM 同时类加载引起内存/CPU 峰值。
for service in snail-job-server ym-system ym-auth ym-dbo; do
  "${compose[@]}" up -d --no-deps --wait --wait-timeout 240 "$service"
done
# 物联网迁移后的业务服务使用固定镜像及旧接入禁用配置。
bash "$script_dir/start-jetlinks-business.sh"
"${compose[@]}" up -d --no-deps --wait --wait-timeout 240 ym-gateway
"${compose[@]}" ps snail-job-server ym-system ym-auth ym-dbo ym-agriculture ym-iot ym-gateway
