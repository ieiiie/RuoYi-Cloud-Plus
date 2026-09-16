#!/usr/bin/env python3
"""Compile ownership slices; use isolated H2 or an explicitly selected disposable MySQL fixture. No application, Dubbo or device connections."""
import argparse, os, subprocess, tempfile
from pathlib import Path
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--classpath-file',type=Path,required=True)
p.add_argument('--rpc-api-root',type=Path,required=True)
p.add_argument('--output',type=Path)
p.add_argument('--mysql-url',help='Opt-in disposable localhost ownership_lock_test schema only')
p.add_argument('--mysql-admin-password-file',type=Path)
p.add_argument('--mysql-password-file',type=Path)
a=p.parse_args()
root=next(x for x in Path(__file__).resolve().parents if (x/'script/sql/jetlinks-ownership.sql').is_file())
out=(a.output or Path(tempfile.mkdtemp(prefix='dbo-ownership-tests-'))).resolve(); out.mkdir(parents=True,exist_ok=True)
cp=[x for x in a.classpath_file.read_text().strip().split(os.pathsep) if not any(y in Path(x).name for y in ['junit-','mockito-','byte-buddy','h2-','jetlinks-iot-rpc-api'])]
for group,name,version in [('org/junit/jupiter','junit-jupiter-api','6.0.3'),('org/junit/jupiter','junit-jupiter-engine','6.0.3'),('org/junit/platform','junit-platform-commons','6.0.3'),('org/junit/platform','junit-platform-engine','6.0.3'),('org/junit/platform','junit-platform-launcher','6.0.3'),('org/mockito','mockito-core','5.23.0'),('net/bytebuddy','byte-buddy','1.18.10'),('net/bytebuddy','byte-buddy-agent','1.18.10'),('org/objenesis','objenesis','3.3'),('org/apiguardian','apiguardian-api','1.1.2'),('org/opentest4j','opentest4j','1.3.0'),('com/h2database','h2','2.4.240')]:
    jar=Path.home()/'.m2/repository'/group/name/version/f'{name}-{version}.jar'
    if not jar.is_file(): p.error(f'Missing cached dependency: {jar}')
    cp.append(str(jar))
mysql_options=[]
test_selector=[]
if a.mysql_url:
    import re
    if not re.fullmatch(r'jdbc:mysql://127\.0\.0\.1:[0-9]+/ownership_lock_test\?.+',a.mysql_url):
        p.error('Only the disposable localhost ownership_lock_test schema is allowed')
    if not a.mysql_admin_password_file or not a.mysql_password_file:
        p.error('Both isolated fixture password files are required')
    drivers=sorted((Path.home()/'.m2/repository/com/mysql/mysql-connector-j').glob('*/*.jar'))
    drivers=[x for x in drivers if not x.name.endswith(('-sources.jar','-javadoc.jar'))]
    if not drivers: p.error('Missing cached MySQL driver')
    cp.append(str(drivers[-1]))
    mysql_options=[f'-Downership.mysql.url={a.mysql_url}',f'-Downership.mysql.adminPasswordFile={a.mysql_admin_password_file.resolve()}',f'-Downership.mysql.passwordFile={a.mysql_password_file.resolve()}']
    test_selector=['com.ym.system.ownership.DeviceOwnershipMySqlLockTest']
cp=os.pathsep.join(dict.fromkeys(cp))
files=list((a.rpc_api_root/'src/main/java').rglob('*.java'))
for module,package in [('ym-iot','com/ym/iot/ownership'),('ym-dbo','com/ym/system/ownership')]:
    files+=list((root/f'ym-modules/{module}/src/main/java'/package).rglob('*.java'))
    files+=list((root/f'ym-modules/{module}/src/test/java'/package).rglob('*.java'))
files.append(Path(__file__).with_name('OwnershipTestLauncher.java'))
commands=[['javac','--release','21','-proc:full','-processor','lombok.launch.AnnotationProcessorHider$AnnotationProcessor','-parameters','-cp',cp,'-d',str(out),*map(str,files)],['java',*mysql_options,'-XX:+EnableDynamicAgentLoading','-Djdk.attach.allowAttachSelf=true','-cp',str(out)+os.pathsep+cp,'OwnershipTestLauncher',*test_selector]]
for name,command in zip(['compile','tests'],commands):
    result=subprocess.run(command,cwd=root,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
    (out/f'{name}.log').write_text(result.stdout); print(result.stdout)
    if result.returncode: raise SystemExit(result.returncode)
print(f'Ownership verification: {out}')
