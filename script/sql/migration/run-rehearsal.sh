#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_dir="$(cd "${script_dir}/../../.." && pwd)"

mysql_container="${MYSQL_CONTAINER:-ym-mysql}"
mysql_user="${MYSQL_USER:-root}"
mysql_password="${MYSQL_PASSWORD:-123456}"
source_platform_db="${SOURCE_PLATFORM_DB:-ym-test}"
source_agriculture_db="${SOURCE_AGRICULTURE_DB:-ym-sf}"
source_iot_db="${SOURCE_IOT_DB:-ym-iot}"
target_platform_base_db="${TARGET_PLATFORM_BASE_DB:-ry-cloud}"

round_no="${1:-}"
if [[ ! "${round_no}" =~ ^[12]$ ]]; then
  echo "usage: $0 <1|2>" >&2
  exit 2
fi

target_platform_db="ry_cloud_rehearsal_${round_no}"
target_agriculture_db="ym_agriculture_rehearsal_${round_no}"
target_iot_db="ym_iot_rehearsal_${round_no}"
mapping_db="ym_migration_rehearsal_${round_no}"
run_id="ym-migration-r${round_no}-$(date -u +%Y%m%dT%H%M%SZ)"
report_dir="${script_dir}/reports/${run_id}"
mkdir -p "${report_dir}"

for db_name in "${source_platform_db}" "${source_agriculture_db}" "${source_iot_db}" \
  "${target_platform_base_db}" "${target_platform_db}" "${target_agriculture_db}" \
  "${target_iot_db}" "${mapping_db}"; do
  if [[ ! "${db_name}" =~ ^[A-Za-z0-9_-]+$ ]]; then
    echo "unsafe database name: ${db_name}" >&2
    exit 2
  fi
done

if [[ "${target_agriculture_db}" != ym_agriculture_rehearsal_* \
   || "${target_iot_db}" != ym_iot_rehearsal_* \
   || "${target_platform_db}" != ry_cloud_rehearsal_* \
   || "${mapping_db}" != ym_migration_rehearsal_* ]]; then
  echo "refusing to operate on non-rehearsal targets" >&2
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

mysql_dump() {
  docker exec -e MYSQL_PWD="${mysql_password}" "${mysql_container}" \
    mysqldump -u"${mysql_user}" --single-transaction --skip-lock-tables \
      --set-gtid-purged=OFF "$@"
}

rehearsal_targets_created=false
cleanup_rehearsal() {
  local code="$?"
  if [[ "${rehearsal_targets_created}" == "true" ]]; then
    echo "[${run_id}] cleanup isolated rehearsal databases after exit ${code}" >&2
    mysql_exec -e "DROP DATABASE IF EXISTS \`${target_platform_db}\`; DROP DATABASE IF EXISTS \`${target_agriculture_db}\`; DROP DATABASE IF EXISTS \`${target_iot_db}\`; DROP DATABASE IF EXISTS \`${mapping_db}\`;" || true
  fi
  echo "rehearsal process exit ${code}" >&2
}
trap cleanup_rehearsal EXIT

render_sql() {
  sed \
    -e "s/\${SOURCE_PLATFORM_DB}/${source_platform_db}/g" \
    -e "s/\${SOURCE_AGRICULTURE_DB}/${source_agriculture_db}/g" \
    -e "s/\${SOURCE_IOT_DB}/${source_iot_db}/g" \
    -e "s/\${TARGET_PLATFORM_DB}/${target_platform_db}/g" \
    -e "s/\${TARGET_AGRICULTURE_DB}/${target_agriculture_db}/g" \
    -e "s/\${TARGET_IOT_DB}/${target_iot_db}/g" \
    -e "s/\${MAPPING_DB}/${mapping_db}/g" "$1"
}

run_rendered_sql() {
  local sql_file="$1"
  local rendered_file="/tmp/$(basename "${sql_file}").${run_id}.sql"
  render_sql "${sql_file}" > "${rendered_file}"
  mysql_input < "${rendered_file}"
}

table_names_from_schema() {
  awk '/^CREATE TABLE `/ { name=$3; gsub(/`/, "", name); print name }' "$1"
}

is_runtime_table() {
  [[ "$1" == "domain_command_idempotency" ]]
}

copy_table() {
  local source_db="$1" target_db="$2" table_name="$3"
  mysql_exec -e "INSERT INTO \`${target_db}\`.\`${table_name}\` SELECT * FROM \`${source_db}\`.\`${table_name}\`;"
}

write_exact_counts() {
  local db_name="$1" output="$2"
  : > "${output}"
  while IFS= read -r table_name; do
    local count
    count="$(mysql_exec -Nse "SELECT COUNT(*) FROM \`${db_name}\`.\`${table_name}\`;" | tail -n 1)"
    printf '%s\t%s\n' "${table_name}" "${count}" >> "${output}"
  done < <(mysql_exec -Nse "SELECT table_name FROM information_schema.tables WHERE table_schema='${db_name}' AND table_type='BASE TABLE' ORDER BY table_name;")
}

write_domain_count_comparison() {
  local output="$1"
  printf 'domain\ttable_name\tsource_rows\ttarget_rows\tdelta\n' > "${output}"
  while IFS=$'\t' read -r domain source_db target_db schema_file; do
    while IFS= read -r table_name; do
      is_runtime_table "${table_name}" && continue
      local source_rows target_rows
      source_rows="$(mysql_exec -Nse "SELECT COUNT(*) FROM \`${source_db}\`.\`${table_name}\`;" | tail -n 1)"
      target_rows="$(mysql_exec -Nse "SELECT COUNT(*) FROM \`${target_db}\`.\`${table_name}\`;" | tail -n 1)"
      printf '%s\t%s\t%s\t%s\t%s\n' \
        "${domain}" "${table_name}" "${source_rows}" "${target_rows}" "$((target_rows - source_rows))" >> "${output}"
    done < <(table_names_from_schema "${schema_file}")
  done <<EOF
agriculture	${source_agriculture_db}	${target_agriculture_db}	${repo_dir}/script/sql/agriculture/ym-agriculture-domain-schema.sql
agriculture-identity	${source_platform_db}	${target_agriculture_db}	${repo_dir}/script/sql/agriculture/ym-agriculture-identity-schema.sql
iot	${source_iot_db}	${target_iot_db}	${repo_dir}/script/sql/iot/ym-iot-domain-schema.sql
EOF
}

echo "[${run_id}] read-only precheck"
run_rendered_sql "${script_dir}/00-precheck.sql" > "${report_dir}/00-precheck.tsv"

echo "[${run_id}] recreate isolated rehearsal databases"
rehearsal_targets_created=true
  mysql_exec -e "DROP DATABASE IF EXISTS \`${target_platform_db}\`; DROP DATABASE IF EXISTS \`${target_agriculture_db}\`; DROP DATABASE IF EXISTS \`${target_iot_db}\`; DROP DATABASE IF EXISTS \`${mapping_db}\`; CREATE DATABASE \`${target_platform_db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE DATABASE \`${target_agriculture_db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE DATABASE \`${target_iot_db}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

echo "[${run_id}] restore clean platform baseline"
mysql_dump "${target_platform_base_db}" | mysql_input "${target_platform_db}"

echo "[${run_id}] create domain schemas"
mysql_input "${target_agriculture_db}" < "${repo_dir}/script/sql/agriculture/ym-agriculture-domain-schema.sql"
mysql_input "${target_agriculture_db}" < "${repo_dir}/script/sql/agriculture/ym-agriculture-identity-schema.sql"
mysql_input "${target_iot_db}" < "${repo_dir}/script/sql/iot/ym-iot-domain-schema.sql"
run_rendered_sql "${script_dir}/10-mapping-schema.sql"
mysql_exec -e "INSERT INTO \`${mapping_db}\`.migration_run(run_id,round_no,started_at,status,source_snapshot_at) VALUES ('${run_id}',${round_no},NOW(),'RUNNING',NOW());"

echo "[${run_id}] build identity maps and stop on conflicts"
run_rendered_sql "${script_dir}/20-build-identity-maps.sql" > "${report_dir}/20-identity-map.tsv"
conflict_count="$(mysql_exec -Nse "SELECT (SELECT COUNT(*) FROM \`${mapping_db}\`.tenant_map WHERE conflict_reason IS NOT NULL) + (SELECT COUNT(*) FROM \`${mapping_db}\`.user_map WHERE conflict_reason IS NOT NULL) + (SELECT COUNT(*) FROM \`${mapping_db}\`.role_map WHERE conflict_reason IS NOT NULL) + (SELECT COUNT(*) FROM \`${mapping_db}\`.menu_map WHERE conflict_reason IS NOT NULL) + (SELECT COUNT(*) FROM \`${mapping_db}\`.oss_map WHERE conflict_reason IS NOT NULL);")"
if [[ "${conflict_count}" != "0" ]]; then
  mysql_exec -e "UPDATE \`${mapping_db}\`.migration_run SET status='FAILED',finished_at=NOW(),failure_reason='identity conflicts' WHERE run_id='${run_id}';"
  echo "identity conflicts: ${conflict_count}; see ${report_dir}/20-identity-map.tsv" >&2
  exit 3
fi

echo "[${run_id}] migrate platform identities and social bindings"
run_rendered_sql "${script_dir}/30-migrate-platform-identity.sql" > "${report_dir}/30-platform-identity.tsv"

echo "[${run_id}] migrate complete agriculture and IoT menu trees"
run_rendered_sql "${script_dir}/35-migrate-business-menu-tree.sql" > "${report_dir}/35-business-menu-tree.tsv"

echo "[${run_id}] expand approved package, role and role-template menu grants"
run_rendered_sql "${script_dir}/36-expand-business-menu-grants.sql" > "${report_dir}/36-business-menu-grants.tsv"

echo "[${run_id}] migrate source tenant packages"
run_rendered_sql "${script_dir}/37-migrate-source-tenant-packages.sql" > "${report_dir}/37-source-tenant-packages.tsv"

echo "[${run_id}] remove compatibility placeholder menus"
run_rendered_sql "${script_dir}/38-remove-compatibility-placeholder-menus.sql" > "${report_dir}/38-remove-placeholder-menus.tsv"

echo "[${run_id}] copy agriculture and IoT domain rows"
while IFS= read -r table_name; do
  is_runtime_table "${table_name}" && continue
  copy_table "${source_agriculture_db}" "${target_agriculture_db}" "${table_name}"
done < <(table_names_from_schema "${repo_dir}/script/sql/agriculture/ym-agriculture-domain-schema.sql")
echo "[${run_id}] agriculture rows copied"
while IFS= read -r table_name; do
  copy_table "${source_platform_db}" "${target_agriculture_db}" "${table_name}"
done < <(table_names_from_schema "${repo_dir}/script/sql/agriculture/ym-agriculture-identity-schema.sql")
echo "[${run_id}] agriculture identity rows copied"
while IFS= read -r table_name; do
  is_runtime_table "${table_name}" && continue
  copy_table "${source_iot_db}" "${target_iot_db}" "${table_name}"
done < <(table_names_from_schema "${repo_dir}/script/sql/iot/ym-iot-domain-schema.sql")
echo "[${run_id}] IoT rows copied"

echo "[${run_id}] rewrite tenant, user and OSS references through maps"
while IFS=$'\t' read -r db_name table_name; do
    mysql_exec -e "UPDATE \`${db_name}\`.\`${table_name}\` d JOIN \`${mapping_db}\`.tenant_map m ON BINARY m.old_tenant_id=BINARY d.tenant_id SET d.tenant_id=m.target_tenant_id;"
done < <(mysql_exec -Nse "SELECT table_schema,table_name FROM information_schema.columns WHERE table_schema IN ('${target_agriculture_db}','${target_iot_db}') AND column_name='tenant_id' ORDER BY table_schema,table_name;")

user_columns="user_id owner_user_id create_by update_by creator_user_id operator_user_id reviewer_user_id publisher_user_id"
for column_name in ${user_columns}; do
  while IFS=$'\t' read -r db_name table_name; do
    mysql_exec -e "UPDATE \`${db_name}\`.\`${table_name}\` d JOIN \`${mapping_db}\`.user_map m ON m.old_user_id=d.\`${column_name}\` SET d.\`${column_name}\`=m.target_user_id WHERE m.conflict_reason IS NULL;"
  done < <(mysql_exec -Nse "SELECT table_schema,table_name FROM information_schema.columns WHERE table_schema IN ('${target_agriculture_db}','${target_iot_db}') AND column_name='${column_name}' AND data_type IN ('bigint','int') ORDER BY table_schema,table_name;")
done

while IFS=$'\t' read -r db_name table_name column_name; do
  mysql_exec -e "UPDATE \`${db_name}\`.\`${table_name}\` d JOIN \`${mapping_db}\`.oss_map m ON m.old_oss_id=d.\`${column_name}\` SET d.\`${column_name}\`=m.target_oss_id WHERE m.conflict_reason IS NULL;"
done < <(mysql_exec -Nse "SELECT table_schema,table_name,column_name FROM information_schema.columns WHERE table_schema='${target_agriculture_db}' AND (column_name LIKE '%oss_id' OR column_name='avatar') AND data_type IN ('bigint','int') ORDER BY table_name,column_name;")

echo "[${run_id}] exact counts and integrity checks"
write_exact_counts "${source_agriculture_db}" "${report_dir}/source-agriculture-counts.tsv"
write_exact_counts "${target_agriculture_db}" "${report_dir}/target-agriculture-counts.tsv"
write_exact_counts "${source_iot_db}" "${report_dir}/source-iot-counts.tsv"
write_exact_counts "${target_iot_db}" "${report_dir}/target-iot-counts.tsv"
write_domain_count_comparison "${report_dir}/domain-row-count-comparison.tsv"
run_rendered_sql "${script_dir}/50-validate.sql" > "${report_dir}/50-validation.tsv"

mysql_exec -e "SELECT 'inventory_balance' AS metric,
  (SELECT COUNT(*) FROM \`${source_agriculture_db}\`.sf_inventory_balance) AS source_count,
  (SELECT COUNT(*) FROM \`${target_agriculture_db}\`.sf_inventory_balance) AS target_count,
  (SELECT COALESCE(SUM(quantity),0) FROM \`${source_agriculture_db}\`.sf_inventory_balance) AS source_quantity,
  (SELECT COALESCE(SUM(quantity),0) FROM \`${target_agriculture_db}\`.sf_inventory_balance) AS target_quantity
UNION ALL SELECT 'inventory_ledger',
  (SELECT COUNT(*) FROM \`${source_agriculture_db}\`.sf_inventory_ledger),
  (SELECT COUNT(*) FROM \`${target_agriculture_db}\`.sf_inventory_ledger),
  (SELECT COALESCE(SUM(quantity_delta),0) FROM \`${source_agriculture_db}\`.sf_inventory_ledger),
  (SELECT COALESCE(SUM(quantity_delta),0) FROM \`${target_agriculture_db}\`.sf_inventory_ledger);" > "${report_dir}/inventory-aggregates.tsv"

source_problem_count="$(mysql_exec -Nse "SELECT
  (SELECT COUNT(*) FROM \`${source_agriculture_db}\`.sf_inventory_balance WHERE quantity < 0) +
  (SELECT COUNT(*) FROM \`${source_agriculture_db}\`.sf_stask_work_order o LEFT JOIN \`${source_agriculture_db}\`.sf_stask_task_package p ON p.package_id=o.package_id WHERE o.package_id IS NOT NULL AND p.package_id IS NULL) +
  (SELECT COUNT(*) FROM \`${source_agriculture_db}\`.sf_stask_dispatch d LEFT JOIN \`${source_agriculture_db}\`.sf_stask_work_order o ON o.order_id=d.order_id WHERE o.order_id IS NULL) +
  (SELECT COUNT(*) FROM \`${source_agriculture_db}\`.sf_field_iot f LEFT JOIN \`${source_iot_db}\`.iot_device d ON d.device_code=f.device_sn WHERE d.device_id IS NULL) +
  (SELECT COUNT(*) FROM \`${source_iot_db}\`.iot_data_point p LEFT JOIN \`${source_iot_db}\`.iot_device d ON d.device_id=p.device_id WHERE d.device_id IS NULL) +
  (SELECT COUNT(*) FROM \`${source_iot_db}\`.iot_alert_record r LEFT JOIN \`${source_iot_db}\`.iot_alert_rule a ON a.rule_id=r.rule_id WHERE r.rule_id IS NOT NULL AND a.rule_id IS NULL);")"
target_problem_count="$(mysql_exec -Nse "SELECT
  (SELECT COUNT(*) FROM \`${target_agriculture_db}\`.sf_inventory_balance WHERE quantity < 0) +
  (SELECT COUNT(*) FROM \`${target_agriculture_db}\`.sf_stask_work_order o LEFT JOIN \`${target_agriculture_db}\`.sf_stask_task_package p ON p.package_id=o.package_id WHERE o.package_id IS NOT NULL AND p.package_id IS NULL) +
  (SELECT COUNT(*) FROM \`${target_agriculture_db}\`.sf_stask_dispatch d LEFT JOIN \`${target_agriculture_db}\`.sf_stask_work_order o ON o.order_id=d.order_id WHERE o.order_id IS NULL) +
  (SELECT COUNT(*) FROM \`${target_agriculture_db}\`.sf_field_iot f LEFT JOIN \`${target_iot_db}\`.iot_device d ON d.device_code=f.device_sn WHERE d.device_id IS NULL) +
  (SELECT COUNT(*) FROM \`${target_iot_db}\`.iot_data_point p LEFT JOIN \`${target_iot_db}\`.iot_device d ON d.device_id=p.device_id WHERE d.device_id IS NULL) +
  (SELECT COUNT(*) FROM \`${target_iot_db}\`.iot_alert_record r LEFT JOIN \`${target_iot_db}\`.iot_alert_rule a ON a.rule_id=r.rule_id WHERE r.rule_id IS NOT NULL AND a.rule_id IS NULL);")"
printf 'source_problem_count\ttarget_problem_count\n%s\t%s\n' "${source_problem_count}" "${target_problem_count}" > "${report_dir}/source-vs-target-quality.tsv"

row_count_mismatch_count="$(awk -F '\t' 'NR > 1 && $5 != 0 { count++ } END { print count + 0 }' "${report_dir}/domain-row-count-comparison.tsv")"

run_status="SUCCEEDED"
run_exit=0
if [[ "${row_count_mismatch_count}" != "0" ]]; then
  run_status="FAILED_VALIDATION"
  run_exit=4
elif [[ "${target_problem_count}" != "${source_problem_count}" ]]; then
  run_status="FAILED_VALIDATION"
  run_exit=4
elif [[ "${source_problem_count}" != "0" ]]; then
  run_status="DONE_SOURCE_ISSUES"
fi
mysql_exec -e "UPDATE \`${mapping_db}\`.migration_run SET status='${run_status}',finished_at=NOW(),failure_reason=IF('${run_status}'='FAILED_VALIDATION',CONCAT('row count mismatches=',${row_count_mismatch_count},'; target/source quality totals differ=',IF(${target_problem_count}=${source_problem_count},0,1)),NULL) WHERE run_id='${run_id}'; SELECT * FROM \`${mapping_db}\`.migration_run WHERE run_id='${run_id}';" > "${report_dir}/run.tsv"

echo "[${run_id}] destroy isolated rehearsal databases"
mysql_exec -e "DROP DATABASE \`${target_platform_db}\`; DROP DATABASE \`${target_agriculture_db}\`; DROP DATABASE \`${target_iot_db}\`; DROP DATABASE \`${mapping_db}\`;"
rehearsal_targets_created=false
echo "[${run_id}] completed; reports: ${report_dir}"
exit "${run_exit}"
