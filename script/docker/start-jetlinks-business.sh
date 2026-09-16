#!/usr/bin/env bash
# Restart only the migrated business services; database initialization is separate.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
export YM_IOT_JETLINKS_IMAGE=${YM_IOT_JETLINKS_IMAGE:-ym-iot:responsibility-20260910-001}
export YM_AGRICULTURE_JETLINKS_IMAGE=${YM_AGRICULTURE_JETLINKS_IMAGE:-ym-agriculture:responsibility-20260910-001}
export YM_DBO_JETLINKS_IMAGE=${YM_DBO_JETLINKS_IMAGE:-ym-dbo:responsibility-20260910-001}
export YM_SYSTEM_JETLINKS_IMAGE=${YM_SYSTEM_JETLINKS_IMAGE:-ym-system:responsibility-20260910-001}
docker image inspect "$YM_IOT_JETLINKS_IMAGE" "$YM_AGRICULTURE_JETLINKS_IMAGE" "$YM_DBO_JETLINKS_IMAGE" "$YM_SYSTEM_JETLINKS_IMAGE" >/dev/null
docker compose -p docker -f docker-compose.yml -f docker-compose.jetlinks.yml up -d --no-deps --wait --wait-timeout 240 ym-system ym-dbo ym-iot ym-agriculture
