#!/usr/bin/env python3
"""Test permission migration against a synthetic menu fixture in NEW network-less MySQL; no source DB reads."""
import argparse,subprocess,time,re,hashlib
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--output',type=Path,required=True);a=p.parse_args()
a.output.mkdir(parents=True,exist_ok=True)
root=next(r for r in Path(__file__).resolve().parents if (r/'script/sql/jetlinks-ownership.sql').exists())
migration=root/'script/sql/jetlinks-http-permissions.sql';script=migration.read_text()
prefix="USE `ry-cloud`; SET @iot_permissions_saas_schema='ry-cloud'; SET @iot_permissions_app_id=1762100000000000102; SET @iot_permissions_agriculture_app_id=1762100000000000101;\n"
log=['Synthetic minimum menu schema; verified IDs/paths, invented names/roles; no production export.', 'MySQL8.4 network none, no published ports, tmpfs only.', 'SQL SHA256 '+hashlib.sha256(migration.read_bytes()).hexdigest()]
cid=None;checks=0
def run(command,text=None):return subprocess.run(command,input=text,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
def sql(text,error=None):
 r=run(['docker','exec','-i',cid,'mysql','-uroot','--batch','--skip-column-names','--default-character-set=utf8mb4'],text)
 if error is None:assert r.returncode==0,r.stdout
 else:
  assert r.returncode!=0 and error in r.stdout,r.stdout
  log.append('Expected refusal: '+r.stdout.strip())
 return r.stdout.strip()
def check(name,condition=True):
 global checks
 assert condition,name
 checks+=1;log.append('PASS '+name);print(log[-1],flush=True)
snapshot="SELECT * FROM `ry-cloud`.sys_menu ORDER BY menu_id; SELECT * FROM `ry-cloud`.sys_role_menu ORDER BY role_id,menu_id;"
def negative(name,mutation,error):
 sql(mutation);before=sql(snapshot);sql(prefix+script,error);check(name+' refuses without overwrite',before==sql(snapshot))
try:
 r=run(['docker','run','-d','--pull','never','--network','none','--tmpfs','/var/lib/mysql','-e','MYSQL_ALLOW_EMPTY_PASSWORD=yes','mysql:8.4','--skip-log-bin','--innodb-buffer-pool-size=64M','--max-connections=10']);assert r.returncode==0,r.stdout
 cid=r.stdout.strip();assert re.fullmatch('[a-f0-9]{64}',cid)
 print('Initializing new isolated synthetic MySQL fixture',flush=True)
 for _ in range(90):
  if 'MySQL init process done. Ready for start up.' in run(['docker','logs',cid]).stdout and run(['docker','exec',cid,'mysqladmin','ping','-uroot','--silent']).returncode==0:break
  time.sleep(1)
 else:raise AssertionError('isolated MySQL not ready')
 pages=[(2036362780382191617,2036362602250100737,'product'),(2036363230619754498,2036362602250100737,'device'),(2036364100237385730,2036362602250100737,'device/log'),(2036363886457905154,2036363426850267138,'rule'),(2036363610095214593,2036363426850267138,'record'),(2042078063734329346,2070032256496316418,'iot'),(2042078150459953154,2070032256496316418,'camera'),(2059842701977833473,2070032256496316418,'iot/fertilizer'),(2061333728416821250,2070032256496316418,'motorvalve'),(2035915490939580417,2034884382739337217,'field')]
 controls=[(2066200000000001001+i,2059842701977833473,'iot:fertilizer:'+name) for i,name in enumerate(['start','stop','emergencyStop','reset','startWithoutReset','paramApply','primeWater','cleanTank'])]
 controls += [(2066200000000002001+i,2061333728416821250,'iot:motorvalve:'+name) for i,name in enumerate(['control','percentControl','batchControl'])]
 controls += [(1761400000000030021,2036363230619754498,'iot:device:assign')]
 sql('''CREATE DATABASE `ry-cloud`; CREATE TABLE `ry-cloud`.sys_menu (menu_id BIGINT PRIMARY KEY,app_id BIGINT,menu_name VARCHAR(64),parent_id BIGINT,order_num INT,path VARCHAR(200),is_frame CHAR(1),is_cache CHAR(1),menu_type CHAR(1),visible CHAR(1),status CHAR(1),perms VARCHAR(100),icon VARCHAR(100),create_time DATETIME,remark VARCHAR(500)); CREATE TABLE `ry-cloud`.sys_role_menu(role_id BIGINT,menu_id BIGINT);
INSERT INTO `ry-cloud`.sys_role_menu VALUES (9001,2036363230619754498),(9002,2066200000000001001);''')
 for id,parent,path in pages:
  app=1762100000000000101 if path=='field' else 1762100000000000102
  sql(f"INSERT INTO `ry-cloud`.sys_menu(menu_id,app_id,menu_name,parent_id,path,menu_type,status) VALUES ({id},{app},'synthetic-page',{parent},'{path}','C','0');")
 for id,parent,perm in controls:sql(f"INSERT INTO `ry-cloud`.sys_menu(menu_id,app_id,menu_name,parent_id,path,menu_type,status,perms) VALUES ({id},1762100000000000102,'synthetic-control',{parent},'','F','0','{perm}');")
 old_control_query='SELECT * FROM `ry-cloud`.sys_menu WHERE menu_id IN ('+','.join(str(c[0]) for c in controls)+') ORDER BY menu_id;'
 page_query="SELECT menu_id,app_id,menu_name,parent_id,order_num,path,is_frame,is_cache,menu_type,visible,status,icon,create_time,remark FROM `ry-cloud`.sys_menu WHERE menu_type='C' ORDER BY menu_id;"
 old_controls=sql(old_control_query);old_pages=sql(page_query);old_roles=sql('SELECT * FROM `ry-cloud`.sys_role_menu ORDER BY role_id,menu_id;')
 sql(prefix+script);check('Full SQL first execution succeeds')
 current=sql(snapshot);sql(prefix+script);check('Full SQL rerun is exactly idempotent',current==sql(snapshot))
 check('Twelve existing control/ownership buttons retain all fields and IDs',old_controls==sql(old_control_query))
 check('Ten page identities unchanged; only their NULL read permissions filled',old_pages==sql(page_query) and sql("SELECT COUNT(*) FROM `ry-cloud`.sys_menu WHERE menu_type='C' AND perms IS NOT NULL;")=='10')
 check('Existing role grants unchanged and no new buttons automatically granted',old_roles==sql('SELECT * FROM `ry-cloud`.sys_role_menu ORDER BY role_id,menu_id;'))
 codes=set()
 for file in (root/'ym-modules/ym-iot/src/main/java/com/ym/iot').rglob('*Controller.java'):
  for annotation in re.findall(r'@SaCheckPermission\(([^)]*)\)',file.read_text()):codes.update(re.findall(r'"([a-zA-Z][\w:]+)"',annotation))
 check('Every one of the 54 HTTP permission codes has a unique menu entry',codes==set(sql('SELECT perms FROM `ry-cloud`.sys_menu WHERE perms IS NOT NULL;').splitlines()) and sql('SELECT COUNT(*) FROM `ry-cloud`.sys_menu;')=='54')
 check('Existing menu holders receive only READ permission; action permissions stay distinct',sql("SELECT perms FROM `ry-cloud`.sys_menu WHERE menu_id=2036363230619754498;")=='iot:device:list')
 before=sql(snapshot);sql(prefix+'SET @iot_permissions_app_id=1;'+script,'Verified page identity');check('Wrong explicit app refuses without modification',before==sql(snapshot))
 sql(prefix+'SET @iot_permissions_app_id=NULL;'+script,'Set explicit verified');check('Missing explicit app refuses without modification',before==sql(snapshot))
 negative('Wrong page path',"UPDATE `ry-cloud`.sys_menu SET path='wrong' WHERE menu_id=2036363230619754498;",'Verified page identity')
 sql("UPDATE `ry-cloud`.sys_menu SET path='device' WHERE menu_id=2036363230619754498;")
 negative('Existing conflicting page permission',"UPDATE `ry-cloud`.sys_menu SET perms='unrelated:read' WHERE menu_id=2036363230619754498;",'Verified page identity')
 sql("UPDATE `ry-cloud`.sys_menu SET perms='iot:device:list' WHERE menu_id=2036363230619754498;")
 negative('NULL app never silently matches',"UPDATE `ry-cloud`.sys_menu SET app_id=NULL WHERE menu_id=2036363230619754498;",'Verified page identity')
 sql("UPDATE `ry-cloud`.sys_menu SET app_id=1762100000000000102 WHERE menu_id=2036363230619754498;")
 negative('Existing action wrong parent',"UPDATE `ry-cloud`.sys_menu SET parent_id=1 WHERE perms='iot:fertilizer:start';",'Existing action permission conflicts')
 sql("UPDATE `ry-cloud`.sys_menu SET parent_id=2059842701977833473 WHERE perms='iot:fertilizer:start';")
 negative('Existing action NULL app',"UPDATE `ry-cloud`.sys_menu SET app_id=NULL WHERE perms='iot:fertilizer:start';",'Existing action permission conflicts')
 sql("UPDATE `ry-cloud`.sys_menu SET app_id=1762100000000000102 WHERE perms='iot:fertilizer:start';")
 negative('Existing action wrong case',"UPDATE `ry-cloud`.sys_menu SET perms='IOT:FERTILIZER:START' WHERE menu_id=2066200000000001001;",'Existing action permission conflicts')
 sql("UPDATE `ry-cloud`.sys_menu SET perms='iot:fertilizer:start' WHERE menu_id=2066200000000001001;")
 negative('Duplicate action permission',"INSERT INTO `ry-cloud`.sys_menu(menu_id,app_id,parent_id,menu_type,perms) VALUES(99,1762100000000000102,2059842701977833473,'F','iot:fertilizer:start');",'Existing action permission conflicts')
 sql('DELETE FROM `ry-cloud`.sys_menu WHERE menu_id=99;')
 negative('Reserved ID collision',"UPDATE `ry-cloud`.sys_menu SET perms='unrelated:action' WHERE perms='iot:device:export'; UPDATE `ry-cloud`.sys_menu SET perms=NULL WHERE menu_id=2036363230619754498;",'Reserved new button ID')
 check('Late action conflict leaves earlier page permission NULL',sql('SELECT COUNT(*) FROM `ry-cloud`.sys_menu WHERE menu_id=2036363230619754498 AND perms IS NULL;')=='1')
 log.append(f'PASS {checks} MySQL permission migration checks')
finally:
 if cid:log.append('Disposed only this new test container, exit '+str(run(['docker','rm','-f',cid]).returncode))
 (a.output/'mysql-tests.log').write_text('\n'.join(log)+'\n')
 print('Evidence: '+str(a.output/'mysql-tests.log'),flush=True)
