#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "${script_dir}/../../.." && pwd)"
mysql_container="${MYSQL_CONTAINER:-ym-mysql}"
mysql_user="${MYSQL_USER:-root}"
mysql_password="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
source_platform_db="${SOURCE_PLATFORM_DB:-ym-test}"
source_agriculture_db="${SOURCE_AGRICULTURE_DB:-ym-sf}"
target_platform_db="${TARGET_PLATFORM_DB:-ry-cloud}"
target_agriculture_db="${TARGET_AGRICULTURE_DB:-ym-agriculture}"
apply_platform_identity="${APPLY_PLATFORM_IDENTITY:-false}"
mapping_db="ym_migration_test_deploy"
run_id="ym-test-target-$(date -u +%Y%m%dT%H%M%SZ)"
report_dir="${script_dir}/reports/${run_id}"
mkdir -p "${report_dir}"

if [[ "${target_agriculture_db}" != "ym-agriculture" || "${mapping_db}" != "ym_migration_test_deploy" ]]; then
  echo "refusing unsafe test target names" >&2
  exit 2
fi

mysql_exec() {
  docker exec -e MYSQL_PWD="${mysql_password}" "${mysql_container}" \
    mysql -u"${mysql_user}" --default-character-set=utf8mb4 --batch --raw "$@"
}

mysql_input() {
  docker exec -i -e MYSQL_PWD="${mysql_password}" "${mysql_container}" \
    mysql -u"${mysql_user}" --default-character-set=utf8mb4 --batch --raw "$@"
}

render_sql() {
  sed \
    -e "s/\${SOURCE_PLATFORM_DB}/${source_platform_db}/g" \
    -e "s/\${SOURCE_AGRICULTURE_DB}/${source_agriculture_db}/g" \
    -e "s/\${SOURCE_IOT_DB}/ym-iot/g" \
    -e "s/\${TARGET_PLATFORM_DB}/${target_platform_db}/g" \
    -e "s/\${TARGET_AGRICULTURE_DB}/${target_agriculture_db}/g" \
    -e "s/\${TARGET_IOT_DB}/ym-iot/g" \
    -e "s/\${MAPPING_DB}/${mapping_db}/g" "$1"
}

run_rendered_sql() {
  local rendered_file="/tmp/$(basename "$1").${run_id}.sql"
  render_sql "$1" > "${rendered_file}"
  mysql_input < "${rendered_file}"
}

table_names_from_schema() {
  awk '/^CREATE TABLE `/ { name=$3; gsub(/`/, "", name); print name }' "$1"
}

is_runtime_table() {
  [[ "$1" == "domain_command_idempotency" ]]
}

if mysql_exec -Nse "SELECT SCHEMA_NAME FROM information_schema.schemata WHERE schema_name='${target_agriculture_db}'" | grep -q .; then
  echo "target database ${target_agriculture_db} already exists; refusing to overwrite" >&2
  exit 3
fi

created=false
cleanup_on_failure() {
  local code="$?"
  if [[ "${code}" != "0" && "${created}" == "true" ]]; then
    mysql_exec -e "DROP DATABASE IF EXISTS \`${target_agriculture_db}\`; DROP DATABASE IF EXISTS \`${mapping_db}\`;" || true
  fi
}
trap cleanup_on_failure EXIT

echo "[${run_id}] precheck latest source snapshot"
run_rendered_sql "${script_dir}/00-precheck.sql" > "${report_dir}/00-precheck.tsv"

echo "[${run_id}] create clean agriculture target and mapping database"
mysql_exec -e "CREATE DATABASE \`${target_agriculture_db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
created=true
mysql_input "${target_agriculture_db}" < "${repo_dir}/script/sql/agriculture/ym-agriculture-domain-schema.sql"
mysql_input "${target_agriculture_db}" < "${repo_dir}/script/sql/agriculture/ym-agriculture-identity-schema.sql"
run_rendered_sql "${script_dir}/10-mapping-schema.sql"

echo "[${run_id}] build and validate identity maps"
run_rendered_sql "${script_dir}/20-build-identity-maps.sql" > "${report_dir}/20-identity-map.tsv"
conflicts="$(mysql_exec -Nse "SELECT
  (SELECT COUNT(*) FROM \`${mapping_db}\`.tenant_map WHERE conflict_reason IS NOT NULL) +
  (SELECT COUNT(*) FROM \`${mapping_db}\`.user_map WHERE conflict_reason IS NOT NULL) +
  (SELECT COUNT(*) FROM \`${mapping_db}\`.role_map WHERE conflict_reason IS NOT NULL) +
  (SELECT COUNT(*) FROM \`${mapping_db}\`.menu_map WHERE conflict_reason IS NOT NULL) +
  (SELECT COUNT(*) FROM \`${mapping_db}\`.oss_map WHERE conflict_reason IS NOT NULL);")"
if [[ "${conflicts}" != "0" ]]; then
  echo "identity conflicts: ${conflicts}" >&2
  exit 4
fi

echo "[${run_id}] copy domain and employee rows"
while IFS= read -r table_name; do
  is_runtime_table "${table_name}" && continue
  mysql_exec -e "INSERT INTO \`${target_agriculture_db}\`.\`${table_name}\` SELECT * FROM \`${source_agriculture_db}\`.\`${table_name}\`;"
done < <(table_names_from_schema "${repo_dir}/script/sql/agriculture/ym-agriculture-domain-schema.sql")
while IFS= read -r table_name; do
  mysql_exec -e "INSERT INTO \`${target_agriculture_db}\`.\`${table_name}\` SELECT * FROM \`${source_platform_db}\`.\`${table_name}\`;"
done < <(table_names_from_schema "${repo_dir}/script/sql/agriculture/ym-agriculture-identity-schema.sql")

echo "[${run_id}] rewrite tenant, user and OSS references"
while IFS=$'\t' read -r db_name table_name; do
  mysql_exec -e "UPDATE \`${db_name}\`.\`${table_name}\` d JOIN \`${mapping_db}\`.tenant_map m ON BINARY m.old_tenant_id=BINARY d.tenant_id SET d.tenant_id=m.target_tenant_id;"
done < <(mysql_exec -Nse "SELECT table_schema,table_name FROM information_schema.columns WHERE table_schema='${target_agriculture_db}' AND column_name='tenant_id' ORDER BY table_name;")

for column_name in user_id owner_user_id create_by update_by creator_user_id operator_user_id reviewer_user_id publisher_user_id; do
  while IFS=$'\t' read -r db_name table_name; do
    mysql_exec -e "UPDATE \`${db_name}\`.\`${table_name}\` d JOIN \`${mapping_db}\`.user_map m ON m.old_user_id=d.\`${column_name}\` SET d.\`${column_name}\`=m.target_user_id WHERE m.conflict_reason IS NULL;"
  done < <(mysql_exec -Nse "SELECT table_schema,table_name FROM information_schema.columns WHERE table_schema='${target_agriculture_db}' AND column_name='${column_name}' AND data_type IN ('bigint','int') ORDER BY table_name;")
done

while IFS=$'\t' read -r db_name table_name column_name; do
  mysql_exec -e "UPDATE \`${db_name}\`.\`${table_name}\` d JOIN \`${mapping_db}\`.oss_map m ON m.old_oss_id=d.\`${column_name}\` SET d.\`${column_name}\`=m.target_oss_id WHERE m.conflict_reason IS NULL;"
done < <(mysql_exec -Nse "SELECT table_schema,table_name,column_name FROM information_schema.columns WHERE table_schema='${target_agriculture_db}' AND (column_name LIKE '%oss_id' OR column_name='avatar') AND data_type IN ('bigint','int') ORDER BY table_name,column_name;")

echo "[${run_id}] verify exact domain row counts"
printf 'table_name\tsource_rows\ttarget_rows\tdelta\n' > "${report_dir}/domain-row-count-comparison.tsv"
mismatches=0
while IFS= read -r table_name; do
  is_runtime_table "${table_name}" && continue
  source_rows="$(mysql_exec -Nse "SELECT COUNT(*) FROM \`${source_agriculture_db}\`.\`${table_name}\`;")"
  target_rows="$(mysql_exec -Nse "SELECT COUNT(*) FROM \`${target_agriculture_db}\`.\`${table_name}\`;")"
  delta="$((target_rows - source_rows))"
  printf '%s\t%s\t%s\t%s\n' "${table_name}" "${source_rows}" "${target_rows}" "${delta}" >> "${report_dir}/domain-row-count-comparison.tsv"
  [[ "${delta}" == "0" ]] || mismatches=$((mismatches + 1))
done < <(table_names_from_schema "${repo_dir}/script/sql/agriculture/ym-agriculture-domain-schema.sql")
if [[ "${mismatches}" != "0" ]]; then
  echo "row-count mismatches: ${mismatches}" >&2
  exit 5
fi

if [[ "${apply_platform_identity}" == "true" ]]; then
  echo "[${run_id}] apply conflict-free platform identity, WeChat and business menu mappings"
  run_rendered_sql "${script_dir}/30-migrate-platform-identity.sql" > "${report_dir}/30-platform-identity.tsv"
  run_rendered_sql "${script_dir}/35-migrate-business-menu-tree.sql" > "${report_dir}/35-business-menu-tree.tsv"
  run_rendered_sql "${script_dir}/36-expand-business-menu-grants.sql" > "${report_dir}/36-business-menu-grants.tsv"
  run_rendered_sql "${script_dir}/37-migrate-source-tenant-packages.sql" > "${report_dir}/37-source-tenant-packages.tsv"
  run_rendered_sql "${script_dir}/38-remove-compatibility-placeholder-menus.sql" > "${report_dir}/38-remove-placeholder-menus.tsv"
else
  printf 'status\treason\nSKIPPED\tAPPLY_PLATFORM_IDENTITY is not true; shared platform unchanged\n' \
    > "${report_dir}/30-platform-identity.tsv"
  printf 'status\treason\nSKIPPED\tAPPLY_PLATFORM_IDENTITY is not true; shared platform unchanged\n' \
    > "${report_dir}/35-business-menu-tree.tsv"
  printf 'status\treason\nSKIPPED\tAPPLY_PLATFORM_IDENTITY is not true; shared platform unchanged\n' \
    > "${report_dir}/36-business-menu-grants.tsv"
  printf 'status\treason\nSKIPPED\tAPPLY_PLATFORM_IDENTITY is not true; shared platform unchanged\n' \
    > "${report_dir}/37-source-tenant-packages.tsv"
  printf 'status\treason\nSKIPPED\tAPPLY_PLATFORM_IDENTITY is not true; shared platform unchanged\n' \
    > "${report_dir}/38-remove-placeholder-menus.tsv"
fi
run_rendered_sql "${script_dir}/50-validate.sql" > "${report_dir}/50-validation.tsv"

mysql_exec -e "SELECT COUNT(*) table_count FROM information_schema.tables WHERE table_schema='${target_agriculture_db}'; SELECT COUNT(*) runtime_command_rows FROM \`${target_agriculture_db}\`.domain_command_idempotency;" > "${report_dir}/target-summary.tsv"
mysql_exec -e "DROP DATABASE \`${mapping_db}\`;"
created=false
echo "[${run_id}] completed; reports: ${report_dir}"
