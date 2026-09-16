# DBO owns device assignment writes

DBO `/saas/iot/device-ownership/**` is the sole writer of the existing IoT ownership/current/history/cleanup tables. Technical profiles remain JetLinks-owned; `iot_device` is only selected and locked. DBO uses its independent platform session and `saas:iot-device-ownership:{query,assign,release,transfer,retry}` permissions. Audit adds `operator_source=DBO` while `operator_tenant_id` stays NULL. Legacy audit rows remain BUSINESS; old IDs are never reinterpreted as DBO users.

## Datasources and rollout

- Explicit physical **iot** datasource; missing/wrong name aborts startup, no master fallback. IoT JdbcTemplate and REQUIRES_NEW READ_COMMITTED transaction manager are tied to that physical connection.
- Explicit physical **saas** JdbcTemplate validates active, not deleted, not expired tenants. No business TenantHelper dependency is added to DBO.
- Template: `src/main/resources/application-ownership-template.yml`; merge `iot` into the existing DBO datasource config without changing master/saas. Required environment secrets: DBO_IOT_JDBC_URL, DBO_IOT_DB_USERNAME, DBO_IOT_DB_PASSWORD. Template is not auto-imported.
- Existing-schema migration: `script/sql/dbo-iot-ownership-audit.sql` (repository root), adds two source columns only, preserves active ownership and unfinished fences. **Do not rerun bootstrap jetlinks-ownership.sql.**
- Privilege manifest generator: `script/sql/dbo-iot-ownership-grants-template.sql`; review generated GRANTs for newly provisioned accounts, never an account with inherited/global/schema write grants. No credentials or runtime mutations are in that generator.
- DBO menu migration: `src/main/resources/db/update/009-add-iot-device-ownership.sql`, default platform administrator only; delegate individual actions explicitly to operations roles.
- Stop old ownership writers before switching runtime identities. ym-iot must get SELECT-only ownership privileges, DBO must have no technical-profile DML.

## Internal RPC

- `IotBusinessGuardRpcService`, group jetlinks-iot-business, version **2.0.0**. `checkOwnershipChange(List<String>)` reads all persisted dependencies and ym-iot local QUEUED/RUNNING fertilizer tasks, deliberately ignores ownership's own FROZEN/SYNC_PENDING. Missing keys/unavailable checks block changes. Archive consumers must also upgrade references to v2.
- `IotCommandRpcService`, jetlinks-iot **1.0.0**, operation fence; caller ym-dbo, operator dbo:<id>, request ID stable by device/version/frozen state.
- `IotAlarmRpcService`, jetlinks-iot **2.0.0**, removeDeviceScope(context, deviceId, previousAssignmentVersion). context carries persisted cleanup requestId, caller ym-dbo, original operator source, and current published assignment version. Native provider must restrict removal to prior business scope and preserve global operations rules.
- All references use zero retries. Provider and discovery transport must remain internal. Current deployment has a single ym-iot instance; before scaling, local task guards must query all instances or rely on a shared durable task registry.

## Failure protocol

Local FROZEN commits before remote effects. Freeze uses V+1; changing or recovering publishes V+2. Current row, history interval and cleanup outbox commit together. Cleanup DONE commits before remote unfreeze. Failures preserve FROZEN or SYNC_PENDING; retry checks the expected version. Released/unassigned devices remain remotely frozen. No automatic task termination, unbinding, or physical controls occur.

Business readers retain `deviceOwnershipJdbc`, `OwnershipActor.tenantId()` and DeviceAccessService. SELECT FOR SHARE locks serialize accepted business operation initiation against DBO SELECT FOR UPDATE; read-side ownership repository contains no DML methods. MySQL shared-lock and database-account privileges require the main runtime integration checks (H2 does not claim to validate MySQL grants).

## Verification

Run `src/test/ownership/run-tests.py --classpath-file <existing business compiler classpath> --rpc-api-root <jetlinks-iot-rpc-api> --output <temp output>`. Compiles the API and both ownership source slices fresh, runs H2/mocked RPC tests; never connects configured databases or devices. Includes existing freeze/version/cleanup recovery regressions relocated from ym-iot.

## SELECT-only technical profile locking

DBO takes `iot_device FOR SHARE` inside the explicit physical IoT write transaction; technical profile permissions remain SELECT-only. Existing ownership rows are locked with `FOR UPDATE`. First ASSIGN may create version 0 using `INSERT IGNORE`, recording REGISTER only when the insert count is 1, then locks and validates the ownership row. Registration, its audit and local freeze share one transaction, so validation/audit failures leave no new row. List/current/history and retry-fence never initialize ownership.

Concurrent duplicate inserts can trigger an InnoDB shared-to-exclusive lock deadlock. Only the pre-remote local freeze transaction retries SQLState 40001 (at most three retries); every retry starts a new transaction after rollback. Remote command fences and alarm cleanup are not automatically retried by this mechanism.

`DeviceOwnershipMySqlLockTest` is opt-in via `ownership.mysql.url` and allows only a disposable localhost `ownership_lock_test` schema, with generated fixture credential files. It verifies SELECT-only profile privileges, SHARE protection against archive, six concurrent first assignments, exactly-once REGISTER audit and rollback. H2 tests deliberately strip unsupported FOR SHARE from the profile read so a fake exclusive profile lock cannot mask broken ownership-row serialization; MySQL tests validate real lock semantics. See `src/test/ownership/run-tests.py --help` for the isolated runner options.
