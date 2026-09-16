-- Test-environment repair approved by the user on 2026-09-04.
-- The supplied source menu dump intentionally had status=1; this is a later
-- explicit activation, not a change to source history or any grant relation.
-- Run against ry-cloud. Re-running is safe. No role/package grants are added.
USE `ry-cloud`;
START TRANSACTION;

SELECT menu_id, parent_id, menu_name, menu_type, status, perms
FROM sys_menu
WHERE menu_id = 207170000000000101
FOR UPDATE;

UPDATE sys_menu
SET status = '0'
WHERE menu_id = 207170000000000101
  AND parent_id = 2057658740529709058
  AND menu_type = 'F'
  AND perms = 'smartfarming:farmWork:form'
  AND status = '1';

SELECT ROW_COUNT() AS changed_rows;
SELECT menu_id, menu_name, status, perms
FROM sys_menu WHERE menu_id = 207170000000000101;
COMMIT;

-- Rollback if requested: restore the known prior disabled state only.
-- UPDATE `ry-cloud`.sys_menu SET status='1'
-- WHERE menu_id=207170000000000101 AND perms='smartfarming:farmWork:form';
