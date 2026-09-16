-- Conservative backfill utility: READ-ONLY gap report, run only when separately authorized.
-- Prerequisite: dbo-iot-ownership-metadata-snapshot.sql has been applied to the existing ym-iot schema.
-- Privilege: SELECT on iot_device_ownership_history only; no technical catalog, event or tenant joins.
-- No trusted historical event/audit source is identified by this migration, so every missing row is
-- reported for evidence review rather than populated from a current device/product or another owner.
-- Do not treat action/operator/reason/effective_to alone as evidence of former display metadata.
-- A future writer must validate archived evidence for this exact device + ownership interval,
-- whitelist the agreed JSON fields, bind parameters, update only metadata_snapshot while it IS NULL,
-- retain provenance outside the snapshot JSON, and never alter interval times/owner/version/fence.
USE `ym-iot`;

-- Counts only; REGISTER/unassigned intervals do not require a display snapshot.
SELECT COUNT(*) AS closed_tenant_intervals,
       COALESCE(SUM(metadata_snapshot IS NULL),0) AS missing_trusted_snapshots
FROM iot_device_ownership_history
WHERE tenant_id IS NOT NULL AND effective_to IS NOT NULL;

-- Keyset page. For each next run set the pair to the LAST returned device_id/assignment_version.
-- These are report cursors, not a backfill cutoff. Never infer completeness from a single page.
SET @snapshot_after_device_id = 0;
SET @snapshot_after_assignment_version = -1;
SELECT device_id,assignment_version,effective_from,effective_to,
       'MISSING_TRUSTED_SNAPSHOT' AS backfill_status
FROM iot_device_ownership_history
WHERE tenant_id IS NOT NULL AND effective_to IS NOT NULL AND metadata_snapshot IS NULL
  AND (device_id > @snapshot_after_device_id
       OR (device_id = @snapshot_after_device_id AND assignment_version > @snapshot_after_assignment_version))
ORDER BY device_id,assignment_version
LIMIT 200;

-- No UPDATE is performed. Missing snapshots remain NULL for the reader's unavailable-history behavior.
-- Output deliberately omits full snapshots, audit free text, credentials and tenant identity.
