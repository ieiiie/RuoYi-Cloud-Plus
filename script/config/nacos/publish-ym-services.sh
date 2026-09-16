#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
nacos_server="${NACOS_SERVER:-http://127.0.0.1:8848}"
nacos_namespace="${NACOS_NAMESPACE:-public}"
nacos_group="${NACOS_GROUP:-DEFAULT_GROUP}"
nacos_access_token="${NACOS_ACCESS_TOKEN:-}"
backup_dir="${NACOS_BACKUP_DIR:-/tmp/ym-nacos-backup-$(date -u +%Y%m%dT%H%M%SZ)}"

if [[ -z "${nacos_access_token}" ]]; then
  echo "NACOS_ACCESS_TOKEN is required; refusing to publish anonymously" >&2
  exit 2
fi

for dependency in curl jq; do
  if ! command -v "${dependency}" >/dev/null 2>&1; then
    echo "missing dependency: ${dependency}" >&2
    exit 2
  fi
done

mkdir -p "${backup_dir}"

data_ids=(
  datasource.yml
  ym-auth.yml
  ym-agriculture.yml
  ym-iot.yml
  ym-agriculture-miniapp.yml
  ym-farm-task-miniapp.yml
  ym-gateway.yml
)

admin_url="${nacos_server%/}/nacos/v3/admin/cs/config"

get_config() {
  local data_id="$1"
  curl --silent --show-error --fail-with-body \
    --get "${admin_url}" \
    --header "accessToken:${nacos_access_token}" \
    --data-urlencode "namespaceId=${nacos_namespace}" \
    --data-urlencode "groupName=${nacos_group}" \
    --data-urlencode "dataId=${data_id}"
}

publish_config() {
  local data_id="$1" source_file="$2"
  curl --silent --show-error --fail-with-body \
    --request POST "${admin_url}" \
    --header "accessToken:${nacos_access_token}" \
    --data-urlencode "namespaceId=${nacos_namespace}" \
    --data-urlencode "groupName=${nacos_group}" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "type=yaml" \
    --data-urlencode "desc=YM agriculture migration managed config" \
    --data-urlencode "content@${source_file}"
}

echo "backing up current Nacos responses to ${backup_dir}"
for data_id in "${data_ids[@]}"; do
  if ! get_config "${data_id}" > "${backup_dir}/${data_id}.json"; then
    # 新 DataId 的不存在响应也是回退依据，不在备份阶段中止。
    printf '{"missing":true,"dataId":"%s"}\n' "${data_id}" > "${backup_dir}/${data_id}.json"
  fi
done

for data_id in "${data_ids[@]}"; do
  source_file="${script_dir}/${data_id}"
  if [[ ! -s "${source_file}" ]]; then
    echo "missing or empty config: ${source_file}" >&2
    exit 3
  fi
  response="$(publish_config "${data_id}" "${source_file}")"
  if ! jq -e '.code == 0 and .data == true' >/dev/null <<< "${response}"; then
    echo "publish failed for ${data_id}: ${response}" >&2
    exit 4
  fi
  echo "published ${data_id}"
done

for data_id in "${data_ids[@]}"; do
  local_content="$(< "${script_dir}/${data_id}")"
  response="$(get_config "${data_id}")"
  remote_content="$(jq -er '.data.content' <<< "${response}")"
  if [[ "${remote_content}" != "${local_content}" ]]; then
    echo "read-back mismatch for ${data_id}" >&2
    exit 5
  fi
  echo "verified ${data_id}"
done

echo "Nacos YM service configs published and verified"
