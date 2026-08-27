-- -----------------------------------------------------------------------------
-- RuoYi-Cloud-Plus MySQL 完整初始化脚本。
-- 已合并 6.0.0 多租户、6.0.1 全局账号、6.0.2 全局字典、6.0.3 菜单清理与
-- 6.0.4 Nacos 控制台菜单清理；新库仅执行本文件，
-- 无需再执行 update_6.0.0-tenant.sql、update_6.0.1-global-user.sql 或
-- update_6.0.2-global-dict.sql、update_6.0.3-remove-system-tool-plus-menu.sql、
-- update_6.0.4-remove-nacos-console-menu.sql。
-- -----------------------------------------------------------------------------

SET NAMES utf8mb4;

-- ----------------------------
-- 第三方平台授权表
-- ----------------------------
create table sys_social
(
    id                 bigint           not null        comment '主键',
    user_id            bigint           not null        comment '用户ID',
    auth_id            varchar(255)     not null        comment '平台+平台唯一id',
    source             varchar(255)     not null        comment '用户来源',
    open_id            varchar(255)     default null    comment '平台编号唯一id',
    user_name          varchar(30)      not null        comment '登录账号',
    nick_name          varchar(30)      default ''      comment '用户昵称',
    email              varchar(255)     default ''      comment '用户邮箱',
    avatar             varchar(500)     default ''      comment '头像地址',
    access_token       varchar(2000)     not null        comment '用户的授权令牌',
    expire_in          int              default null    comment '用户的授权令牌的有效期，部分平台可能没有',
    refresh_token      varchar(2000)     default null    comment '刷新令牌，部分平台可能没有',
    access_code        varchar(255)     default null    comment '平台的授权信息，部分平台可能没有',
    union_id           varchar(255)     default null    comment '用户的 unionid',
    scope              varchar(255)     default null    comment '授予的权限，部分平台可能没有',
    token_type         varchar(255)     default null    comment '个别平台的授权信息，部分平台可能没有',
    id_token           varchar(2000)    default null    comment 'id token，部分平台可能没有',
    mac_algorithm      varchar(255)     default null    comment '小米平台用户的附带属性，部分平台可能没有',
    mac_key            varchar(255)     default null    comment '小米平台用户的附带属性，部分平台可能没有',
    code               varchar(255)     default null    comment '用户的授权code，部分平台可能没有',
    oauth_token        varchar(255)     default null    comment 'Twitter平台用户的附带属性，部分平台可能没有',
    oauth_token_secret varchar(255)     default null    comment 'Twitter平台用户的附带属性，部分平台可能没有',
    create_dept        bigint(20)                       comment '创建部门',
    create_by          bigint(20)                       comment '创建者',
    create_time        datetime                         comment '创建时间',
    update_by          bigint(20)                       comment '更新者',
    update_time        datetime                         comment '更新时间',
    del_flag           char(1)          default '0'     comment '删除标志（0代表存在 1代表删除）',
    PRIMARY KEY (id)
) engine=innodb comment = '社会化关系表';

-- ----------------------------
-- 全局账号表：密码与基础资料只保存一份，不参与 tenant_id 行级隔离
-- ----------------------------
create table sys_global_user (
  global_user_id bigint(20)      not null                   comment '全局账号ID',
  user_name      varchar(30)     not null                   comment '用户账号',
  nick_name      varchar(30)     not null                   comment '用户昵称',
  user_type      varchar(10)     not null default 'sys_user' comment '用户类型',
  email          varchar(50)     default null               comment '用户邮箱',
  phone_number   varchar(11)     default null               comment '手机号码',
  gender         char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
  avatar         bigint(20)      default null               comment '头像 OSS ID',
  password       varchar(100)    not null                   comment '密码',
  status         char(1)         not null default '0'        comment '账号状态（0正常 1停用）',
  del_flag       char(1)         not null default '0'        comment '删除标志（0代表存在 1代表删除）',
  create_dept    bigint(20)      default null               comment '创建部门',
  create_by      bigint(20)      default null               comment '创建者',
  create_time    datetime                                   comment '创建时间',
  update_by      bigint(20)      default null               comment '更新者',
  update_time    datetime                                   comment '更新时间',
  remark         varchar(500)    default null               comment '备注',
  primary key (global_user_id),
  unique key uk_sys_global_user_name  (user_name),
  unique key uk_sys_global_user_phone (phone_number),
  unique key uk_sys_global_user_email (email)
) engine=innodb comment = '全局账号表';

-- ----------------------------
-- 全局第三方账号绑定表：社交与小程序登录先定位全局账号
-- ----------------------------
create table sys_global_social (
  id                 bigint(20)      not null                   comment '主键',
  global_user_id     bigint(20)      not null                   comment '全局账号ID',
  auth_id            varchar(255)    not null                   comment '平台+平台唯一id',
  source             varchar(255)    not null                   comment '用户来源',
  open_id            varchar(255)    default null               comment '平台编号唯一id',
  user_name          varchar(30)     default null               comment '登录账号',
  nick_name          varchar(30)     default null               comment '用户昵称',
  email              varchar(255)    default null               comment '用户邮箱',
  avatar             varchar(500)    default null               comment '头像地址',
  access_token       varchar(2000)   default null               comment '用户的授权令牌',
  expire_in          int             default null               comment '用户的授权令牌有效期',
  refresh_token      varchar(2000)   default null               comment '刷新令牌',
  access_code        varchar(255)    default null               comment '平台授权信息',
  union_id           varchar(255)    default null               comment '用户 unionid',
  scope              varchar(255)    default null               comment '授权范围',
  token_type         varchar(255)    default null               comment '令牌类型',
  id_token           varchar(2000)   default null               comment 'id token',
  mac_algorithm      varchar(255)    default null               comment '小米平台算法',
  mac_key            varchar(255)    default null               comment '小米平台密钥',
  code               varchar(255)    default null               comment '授权 code',
  oauth_token        varchar(255)    default null               comment 'OAuth token',
  oauth_token_secret varchar(255)    default null               comment 'OAuth token secret',
  create_dept        bigint(20)      default null               comment '创建部门',
  create_by          bigint(20)      default null               comment '创建者',
  create_time        datetime                                   comment '创建时间',
  update_by          bigint(20)      default null               comment '更新者',
  update_time        datetime                                   comment '更新时间',
  del_flag           char(1)         not null default '0'        comment '删除标志（0代表存在 1代表删除）',
  primary key (id),
  unique key uk_sys_global_social_auth (source, auth_id),
  unique key uk_sys_global_social_open (open_id),
  key idx_sys_global_social_user (global_user_id)
) engine=innodb comment = '全局第三方账号绑定表';

-- ----------------------------
-- 1、部门表
-- ----------------------------
create table sys_dept (
  dept_id           bigint(20)      not null                   comment '部门id',
  parent_id         bigint(20)      default 0                  comment '父部门id',
  ancestors         varchar(500)    default ''                 comment '祖级列表',
  dept_name         varchar(30)     default ''                 comment '部门名称',
  dept_category     varchar(100)    default null               comment '部门类别编码',
  order_num         int(4)          default 0                  comment '显示顺序',
  leader            bigint(20)      default null               comment '负责人',
  phone             varchar(11)     default null               comment '联系电话',
  email             varchar(50)     default null               comment '邮箱',
  status            char(1)         default '0'                comment '部门状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
  create_dept       bigint(20)      default null               comment '创建部门',
  create_by         bigint(20)      default null               comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         bigint(20)      default null               comment '更新者',
  update_time       datetime                                   comment '更新时间',
  primary key (dept_id),
  key idx_sys_dept_parent_id (parent_id)
) engine=innodb comment = '部门表';

-- ----------------------------
-- 初始化-部门表数据
-- ----------------------------


insert into sys_dept values(1761000000000000100, 0, '0', 'XXX科技', null, 0, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000101, 1761000000000000100, '0,1761000000000000100', '深圳总公司', null, 1, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000102, 1761000000000000100, '0,1761000000000000100', '长沙分公司', null, 2, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000103, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '研发部门', null, 1, 1761100000000000001, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000104, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '市场部门', null, 2, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000105, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '测试部门', null, 3, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000106, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '财务部门', null, 4, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000107, 1761000000000000101, '0,1761000000000000100,1761000000000000101', '运维部门', null, 5, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000108, 1761000000000000102, '0,1761000000000000100,1761000000000000102', '市场部门', null, 1, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);
insert into sys_dept values(1761000000000000109, 1761000000000000102, '0,1761000000000000100,1761000000000000102', '财务部门', null, 2, null, '15888888888', 'xxx@qq.com', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null);


-- ----------------------------
-- 2、用户信息表
-- ----------------------------
create table sys_user (
  user_id           bigint(20)      not null                   comment '用户ID',
  global_user_id    bigint(20)      default null               comment '全局账号ID',
  dept_id           bigint(20)      default null               comment '部门ID',
  nick_name         varchar(30)     not null                   comment '用户昵称',
  user_type         varchar(10)     default 'sys_user'         comment '用户类型（sys_user系统用户）',
  email             varchar(50)     default ''                 comment '用户邮箱',
  gender            char(1)         default '0'                comment '用户性别（0男 1女 2未知）',
  avatar            bigint(20)                                 comment '头像地址',
  status            char(1)         default '0'                comment '账号状态（0正常 1停用）',
  del_flag          char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
  login_ip          varchar(128)    default ''                 comment '最后登录IP',
  login_date        datetime                                   comment '最后登录时间',
  create_dept       bigint(20)      default null               comment '创建部门',
  create_by         bigint(20)      default null               comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         bigint(20)      default null               comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (user_id),
  key idx_sys_user_dept_id   (dept_id),
  key idx_sys_user_create_by (create_by),
  key idx_sys_user_global_user_id (global_user_id)
) engine=innodb comment = '用户信息表';

-- ----------------------------
-- 初始化-用户信息表数据
-- ----------------------------
insert into sys_user (user_id, dept_id, nick_name, user_type, email, gender, avatar, status, del_flag,
                      login_ip, login_date, create_dept, create_by, create_time, update_by, update_time,
                      remark, global_user_id)
values (1761100000000000001, 1761000000000000103, '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '1', null, '0', '0',
        '127.0.0.1', sysdate(), 1761000000000000103, 1761100000000000001, sysdate(), null, null,
        '管理员', 1761100000000000001);
insert into sys_user (user_id, dept_id, nick_name, user_type, email, gender, avatar, status, del_flag,
                      login_ip, login_date, create_dept, create_by, create_time, update_by, update_time,
                      remark, global_user_id)
values (1761100000000000003, 1761000000000000108, '本部门及以下 密码666666', 'sys_user', '', '0', null, '0', '0',
        '127.0.0.1', sysdate(), 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000003, sysdate(),
        null, 1761100000000000003);
insert into sys_user (user_id, dept_id, nick_name, user_type, email, gender, avatar, status, del_flag,
                      login_ip, login_date, create_dept, create_by, create_time, update_by, update_time,
                      remark, global_user_id)
values (1761100000000000004, 1761000000000000102, '仅本人 密码666666', 'sys_user', '', '0', null, '0', '0',
        '127.0.0.1', sysdate(), 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000004, sysdate(),
        null, 1761100000000000004);

-- 初始化全局账号（全量脚本的示例用户与本地 sys_user 一一对应）。
insert into sys_global_user values(1761100000000000001, 'admin', '疯狂的狮子Li', 'sys_user', 'crazyLionLi@163.com', '15888888888', '1', null, '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '管理员');
insert into sys_global_user values(1761100000000000003, 'test', '本部门及以下 密码666666', 'sys_user', null, null, '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000003, sysdate(), null);
insert into sys_global_user values(1761100000000000004, 'test1', '仅本人 密码666666', 'sys_user', null, null, '0', null, '$2a$10$b8yUzN0C71sbz.PhNOCgJe.Tu1yWC3RNrTyjSQ8p1W0.aaUXUJ.Ne', '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000004, sysdate(), null);

-- ----------------------------
-- 3、岗位信息表
-- ----------------------------
create table sys_post
(
  post_id       bigint(20)      not null                   comment '岗位ID',
  dept_id       bigint(20)      not null                   comment '部门id',
  post_code     varchar(64)     not null                   comment '岗位编码',
  post_category varchar(100)    default null               comment '岗位类别编码',
  post_name     varchar(50)     not null                   comment '岗位名称',
  post_sort     int(4)          not null                   comment '显示顺序',
  status        char(1)         not null                   comment '状态（0正常 1停用）',
  del_flag      char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
  create_dept   bigint(20)      default null               comment '创建部门',
  create_by     bigint(20)      default null               comment '创建者',
  create_time   datetime                                   comment '创建时间',
  update_by     bigint(20)      default null               comment '更新者',
  update_time   datetime                                   comment '更新时间',
  remark        varchar(500)    default null               comment '备注',
  primary key (post_id),
  key idx_sys_post_dept_id (dept_id)
) engine=innodb comment = '岗位信息表';

-- ----------------------------
-- 初始化-岗位信息表数据
-- ----------------------------
insert into sys_post values(1761200000000000001, 1761000000000000103, 'ceo', null, '董事长', 1, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_post values(1761200000000000002, 1761000000000000100, 'se', null, '项目经理', 2, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_post values(1761200000000000003, 1761000000000000100, 'hr', null, '人力资源', 3, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_post values(1761200000000000004, 1761000000000000100, 'user', null, '普通员工', 4, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');


-- ----------------------------
-- 4、角色信息表
-- ----------------------------
create table sys_role (
  role_id              bigint(20)      not null                   comment '角色ID',
  role_name            varchar(30)     not null                   comment '角色名称',
  role_key             varchar(100)    not null                   comment '角色权限字符串',
  role_sort            int(4)          not null                   comment '显示顺序',
  data_scope           char(1)         default '1'                comment '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限 6：部门及以下或本人数据权限）',
  menu_check_strictly  tinyint(1)      default 1                  comment '菜单树选择项是否关联显示',
  dept_check_strictly  tinyint(1)      default 1                  comment '部门树选择项是否关联显示',
  status               char(1)         not null                   comment '角色状态（0正常 1停用）',
  del_flag             char(1)         default '0'                comment '删除标志（0代表存在 1代表删除）',
  create_dept          bigint(20)      default null               comment '创建部门',
  create_by            bigint(20)      default null               comment '创建者',
  create_time          datetime                                   comment '创建时间',
  update_by            bigint(20)      default null               comment '更新者',
  update_time          datetime                                   comment '更新时间',
  remark               varchar(500)    default null               comment '备注',
  primary key (role_id),
  key idx_sys_role_create_dept (create_dept),
  key idx_sys_role_create_by   (create_by)
) engine=innodb comment = '角色信息表';

-- ----------------------------
-- 初始化-角色信息表数据
-- ----------------------------
insert into sys_role values(1761300000000000001, '超级管理员', 'superadmin', 1, 1, 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '超级管理员');
insert into sys_role values(1761300000000000003, '本部门及以下', 'test1', 3, 4, 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_role values(1761300000000000004, '仅本人', 'test2', 4, 5, 1, 1, '0', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- ----------------------------
-- 5、菜单权限表
-- ----------------------------
create table sys_menu (
  menu_id           bigint(20)      not null                   comment '菜单ID',
  menu_name         varchar(50)     not null                   comment '菜单名称',
  parent_id         bigint(20)      default 0                  comment '父菜单ID',
  order_num         int(4)          default 0                  comment '显示顺序',
  path              varchar(200)    default ''                 comment '路由地址',
  component         varchar(255)    default null               comment '组件路径',
  query_param       varchar(255)    default null               comment '路由参数',
  is_frame          char(1)         default 'N'                comment '是否为外链（Y是 N否）',
  is_cache          char(1)         default 'Y'                comment '是否缓存（Y缓存 N不缓存）',
  menu_type         char(1)         default ''                 comment '菜单类型（M目录 C菜单 F按钮）',
  visible           char(1)         default 0                  comment '显示状态（0显示 1隐藏）',
  status            char(1)         default 0                  comment '菜单状态（0正常 1停用）',
  perms             varchar(100)    default null               comment '权限标识',
  icon              varchar(100)    default '#'                comment '菜单图标',
  active_menu       varchar(255)    default ''                 comment '激活菜单路径',
  ext               varchar(2000)   default ''                 comment '扩展字段',
  create_dept       bigint(20)      default null               comment '创建部门',
  create_by         bigint(20)      default null               comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         bigint(20)      default null               comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default ''                 comment '备注',
  primary key (menu_id)
) engine=innodb comment = '菜单权限表';

-- ----------------------------
-- 初始化-菜单信息表数据
-- ----------------------------
-- 一级菜单
insert into sys_menu values(1761400000000000001, '系统管理', 0, 1, 'system', null, '', 'N', 'Y', 'M', '0', '0', '', 'system', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '系统管理目录');
insert into sys_menu values(1761400000000000002, '系统监控', 0, 3, 'monitor', null, '', 'N', 'Y', 'M', '0', '0', '', 'monitor', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '系统监控目录');
-- 二级菜单
insert into sys_menu values(1761400000000000100, '用户管理', 1761400000000000001, 1, 'user', 'system/user/index', '', 'N', 'Y', 'C', '0', '0', 'system:user:list', 'user', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '用户管理菜单');
insert into sys_menu values(1761400000000000101, '角色管理', 1761400000000000001, 2, 'role', 'system/role/index', '', 'N', 'Y', 'C', '0', '0', 'system:role:list', 'peoples', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '角色管理菜单');
insert into sys_menu values(1761400000000000102, '菜单管理', 1761400000000000001, 3, 'menu', 'system/menu/index', '', 'N', 'Y', 'C', '0', '0', 'system:menu:list', 'tree-table', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '菜单管理菜单');
insert into sys_menu values(1761400000000000103, '部门管理', 1761400000000000001, 4, 'dept', 'system/dept/index', '', 'N', 'Y', 'C', '0', '0', 'system:dept:list', 'tree', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '部门管理菜单');
insert into sys_menu values(1761400000000000104, '岗位管理', 1761400000000000001, 5, 'post', 'system/post/index', '', 'N', 'Y', 'C', '0', '0', 'system:post:list', 'post', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '岗位管理菜单');
insert into sys_menu values(1761400000000000105, '全局字典', 1761400000000000001, 6, 'dict', 'system/dict/index', '', 'N', 'Y', 'C', '0', '0', 'system:dict:list', 'dict', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '全局字典菜单');
insert into sys_menu values(1761400000000000106, '参数设置', 1761400000000000001, 7, 'config', 'system/config/index', '', 'N', 'Y', 'C', '0', '0', 'system:config:list', 'edit', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '参数设置菜单');
insert into sys_menu values(1761400000000000107, '通知公告', 1761400000000000001, 8, 'notice', 'system/notice/index', '', 'N', 'Y', 'C', '0', '0', 'system:notice:list', 'message', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '通知公告菜单');
insert into sys_menu values(1761400000000000108, '日志管理', 1761400000000000001, 9, 'log', '', '', 'N', 'Y', 'M', '0', '0', '', 'log', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '日志管理菜单');
insert into sys_menu values(1761400000000000109, '在线用户', 1761400000000000002, 1, 'online', 'monitor/online/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:online:list', 'online', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '在线用户菜单');
insert into sys_menu values(1761400000000000113, '缓存监控', 1761400000000000002, 5, 'cache', 'monitor/cache/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:cache:list', 'redis', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '缓存监控菜单');
insert into sys_menu values(1761400000000000123, '客户端管理', 1761400000000000001, 11, 'client', 'system/client/index', '', 'N', 'Y', 'C', '0', '0', 'system:client:list', 'international', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '客户端管理菜单');
insert into sys_menu values(1761400000000000130, '分配用户', 1761400000000000001, 2, 'role-auth/user/:roleId', 'system/role/authUser', '', 'N', 'N', 'C', '1', '0', 'system:role:edit', '#', '/system/role', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000000131, '分配角色', 1761400000000000001, 1, 'user-auth/role/:userId', 'system/user/authRole', '', 'N', 'N', 'C', '1', '0', 'system:user:edit', '#', '/system/user', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000000133, '文件配置管理', 1761400000000000001, 10, 'oss-config/index', 'system/oss/config', '', 'N', 'N', 'C', '1', '0', 'system:ossConfig:list', '#', '/system/oss', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- oss菜单
insert into sys_menu values(1761400000000000118, '文件管理', 1761400000000000001, 10, 'oss', 'system/oss/index', '', 'N', 'Y', 'C', '0', '0', 'system:oss:list', 'upload', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '文件管理菜单');

-- 三级菜单
insert into sys_menu values(1761400000000000500, '操作日志', 1761400000000000108, 1, 'operlog', 'monitor/operlog/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:operlog:list', 'form', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '操作日志菜单');
insert into sys_menu values(1761400000000000501, '登录日志', 1761400000000000108, 2, 'logininfo', 'monitor/logininfo/index', '', 'N', 'Y', 'C', '0', '0', 'monitor:logininfo:list', 'logininfo', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '登录日志菜单');
-- 用户管理按钮
insert into sys_menu values(1761400000000001001, '用户查询', 1761400000000000100, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001002, '用户新增', 1761400000000000100, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001003, '用户修改', 1761400000000000100, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001004, '用户删除', 1761400000000000100, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001005, '用户导出', 1761400000000000100, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001006, '用户导入', 1761400000000000100, 6, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:import', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001007, '重置密码', 1761400000000000100, 7, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:user:resetPwd', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 角色管理按钮
insert into sys_menu values(1761400000000001008, '角色查询', 1761400000000000101, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001009, '角色新增', 1761400000000000101, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001010, '角色修改', 1761400000000000101, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001011, '角色删除', 1761400000000000101, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001012, '角色导出', 1761400000000000101, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:role:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 菜单管理按钮
insert into sys_menu values(1761400000000001013, '菜单查询', 1761400000000000102, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001014, '菜单新增', 1761400000000000102, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001015, '菜单修改', 1761400000000000102, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001016, '菜单删除', 1761400000000000102, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:menu:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 部门管理按钮
insert into sys_menu values(1761400000000001017, '部门查询', 1761400000000000103, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001018, '部门新增', 1761400000000000103, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001019, '部门修改', 1761400000000000103, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001020, '部门删除', 1761400000000000103, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:dept:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 岗位管理按钮
insert into sys_menu values(1761400000000001021, '岗位查询', 1761400000000000104, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001022, '岗位新增', 1761400000000000104, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001023, '岗位修改', 1761400000000000104, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001024, '岗位删除', 1761400000000000104, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001025, '岗位导出', 1761400000000000104, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:post:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 全局字典按钮
insert into sys_menu values(1761400000000001026, '字典查询', 1761400000000000105, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001027, '字典新增', 1761400000000000105, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001028, '字典修改', 1761400000000000105, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001029, '字典删除', 1761400000000000105, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001030, '字典导出', 1761400000000000105, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:dict:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 参数设置按钮
insert into sys_menu values(1761400000000001031, '参数查询', 1761400000000000106, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001032, '参数新增', 1761400000000000106, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001033, '参数修改', 1761400000000000106, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001034, '参数删除', 1761400000000000106, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001035, '参数导出', 1761400000000000106, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:config:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 通知公告按钮
insert into sys_menu values(1761400000000001036, '公告查询', 1761400000000000107, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001037, '公告新增', 1761400000000000107, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001038, '公告修改', 1761400000000000107, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001039, '公告删除', 1761400000000000107, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:notice:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 操作日志按钮
insert into sys_menu values(1761400000000001040, '操作查询', 1761400000000000500, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:operlog:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001041, '操作删除', 1761400000000000500, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:operlog:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001042, '日志导出', 1761400000000000500, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:operlog:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 登录日志按钮
insert into sys_menu values(1761400000000001043, '登录查询', 1761400000000000501, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001044, '登录删除', 1761400000000000501, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001045, '日志导出', 1761400000000000501, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001050, '账户解锁', 1761400000000000501, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:logininfo:unlock', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 在线用户按钮
insert into sys_menu values(1761400000000001046, '在线查询', 1761400000000000109, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:online:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001047, '批量强退', 1761400000000000109, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:online:batchLogout', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001048, '单条强退', 1761400000000000109, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'monitor:online:forceLogout', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- 代码生成按钮
-- oss相关按钮
insert into sys_menu values(1761400000000001600, '文件查询', 1761400000000000118, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001601, '文件上传', 1761400000000000118, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:upload', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001602, '文件下载', 1761400000000000118, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:download', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001603, '文件删除', 1761400000000000118, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:oss:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001620, '配置列表', 1761400000000000118, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:list', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001621, '配置添加', 1761400000000000118, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001622, '配置编辑', 1761400000000000118, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001623, '配置删除', 1761400000000000118, 6, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:ossConfig:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');

-- 客户端管理按钮
insert into sys_menu values(1761400000000001061, '客户端管理查询', 1761400000000000123, 1, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:query', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001062, '客户端管理新增', 1761400000000000123, 2, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:add', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001063, '客户端管理修改', 1761400000000000123, 3, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:edit', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001064, '客户端管理删除', 1761400000000000123, 4, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:remove', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
insert into sys_menu values(1761400000000001065, '客户端管理导出', 1761400000000000123, 5, '#', '', '', 'N', 'Y', 'F', '0', '0', 'system:client:export', '#', '', '', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '');
-- ----------------------------
-- 6、用户和角色关联表  用户N-1角色
-- ----------------------------
create table sys_user_role (
  user_id   bigint(20) not null comment '用户ID',
  role_id   bigint(20) not null comment '角色ID',
  primary key(user_id, role_id),
  key idx_sys_user_role_rid (role_id)
) engine=innodb comment = '用户和角色关联表';

-- ----------------------------
-- 初始化-用户和角色关联表数据
-- ----------------------------
insert into sys_user_role values (1761100000000000001, 1761300000000000001);
insert into sys_user_role values (1761100000000000003, 1761300000000000003);
insert into sys_user_role values (1761100000000000004, 1761300000000000004);

-- ----------------------------
-- 7、角色和菜单关联表  角色1-N菜单
-- ----------------------------
create table sys_role_menu (
  role_id   bigint(20) not null comment '角色ID',
  menu_id   bigint(20) not null comment '菜单ID',
  primary key(role_id, menu_id)
) engine=innodb comment = '角色和菜单关联表';

-- ----------------------------
-- 初始化-角色和菜单关联表数据
-- ----------------------------
insert into sys_role_menu values (1761300000000000003, 1761400000000000001);
insert into sys_role_menu values (1761300000000000003, 1761400000000000005);
insert into sys_role_menu values (1761300000000000003, 1761400000000000100);
insert into sys_role_menu values (1761300000000000003, 1761400000000000101);
insert into sys_role_menu values (1761300000000000003, 1761400000000000102);
insert into sys_role_menu values (1761300000000000003, 1761400000000000103);
insert into sys_role_menu values (1761300000000000003, 1761400000000000104);
insert into sys_role_menu values (1761300000000000003, 1761400000000000105);
insert into sys_role_menu values (1761300000000000003, 1761400000000000106);
insert into sys_role_menu values (1761300000000000003, 1761400000000000107);
insert into sys_role_menu values (1761300000000000003, 1761400000000000108);
insert into sys_role_menu values (1761300000000000003, 1761400000000000118);
insert into sys_role_menu values (1761300000000000003, 1761400000000000123);
insert into sys_role_menu values (1761300000000000003, 1761400000000000130);
insert into sys_role_menu values (1761300000000000003, 1761400000000000131);
insert into sys_role_menu values (1761300000000000003, 1761400000000000133);
insert into sys_role_menu values (1761300000000000003, 1761400000000000500);
insert into sys_role_menu values (1761300000000000003, 1761400000000000501);
insert into sys_role_menu values (1761300000000000003, 1761400000000001001);
insert into sys_role_menu values (1761300000000000003, 1761400000000001002);
insert into sys_role_menu values (1761300000000000003, 1761400000000001003);
insert into sys_role_menu values (1761300000000000003, 1761400000000001004);
insert into sys_role_menu values (1761300000000000003, 1761400000000001005);
insert into sys_role_menu values (1761300000000000003, 1761400000000001006);
insert into sys_role_menu values (1761300000000000003, 1761400000000001007);
insert into sys_role_menu values (1761300000000000003, 1761400000000001008);
insert into sys_role_menu values (1761300000000000003, 1761400000000001009);
insert into sys_role_menu values (1761300000000000003, 1761400000000001010);
insert into sys_role_menu values (1761300000000000003, 1761400000000001011);
insert into sys_role_menu values (1761300000000000003, 1761400000000001012);
insert into sys_role_menu values (1761300000000000003, 1761400000000001013);
insert into sys_role_menu values (1761300000000000003, 1761400000000001014);
insert into sys_role_menu values (1761300000000000003, 1761400000000001015);
insert into sys_role_menu values (1761300000000000003, 1761400000000001016);
insert into sys_role_menu values (1761300000000000003, 1761400000000001017);
insert into sys_role_menu values (1761300000000000003, 1761400000000001018);
insert into sys_role_menu values (1761300000000000003, 1761400000000001019);
insert into sys_role_menu values (1761300000000000003, 1761400000000001020);
insert into sys_role_menu values (1761300000000000003, 1761400000000001021);
insert into sys_role_menu values (1761300000000000003, 1761400000000001022);
insert into sys_role_menu values (1761300000000000003, 1761400000000001023);
insert into sys_role_menu values (1761300000000000003, 1761400000000001024);
insert into sys_role_menu values (1761300000000000003, 1761400000000001025);
insert into sys_role_menu values (1761300000000000003, 1761400000000001026);
insert into sys_role_menu values (1761300000000000003, 1761400000000001027);
insert into sys_role_menu values (1761300000000000003, 1761400000000001028);
insert into sys_role_menu values (1761300000000000003, 1761400000000001029);
insert into sys_role_menu values (1761300000000000003, 1761400000000001030);
insert into sys_role_menu values (1761300000000000003, 1761400000000001031);
insert into sys_role_menu values (1761300000000000003, 1761400000000001032);
insert into sys_role_menu values (1761300000000000003, 1761400000000001033);
insert into sys_role_menu values (1761300000000000003, 1761400000000001034);
insert into sys_role_menu values (1761300000000000003, 1761400000000001035);
insert into sys_role_menu values (1761300000000000003, 1761400000000001036);
insert into sys_role_menu values (1761300000000000003, 1761400000000001037);
insert into sys_role_menu values (1761300000000000003, 1761400000000001038);
insert into sys_role_menu values (1761300000000000003, 1761400000000001039);
insert into sys_role_menu values (1761300000000000003, 1761400000000001040);
insert into sys_role_menu values (1761300000000000003, 1761400000000001041);
insert into sys_role_menu values (1761300000000000003, 1761400000000001042);
insert into sys_role_menu values (1761300000000000003, 1761400000000001043);
insert into sys_role_menu values (1761300000000000003, 1761400000000001044);
insert into sys_role_menu values (1761300000000000003, 1761400000000001045);
insert into sys_role_menu values (1761300000000000003, 1761400000000001050);
insert into sys_role_menu values (1761300000000000003, 1761400000000001061);
insert into sys_role_menu values (1761300000000000003, 1761400000000001062);
insert into sys_role_menu values (1761300000000000003, 1761400000000001063);
insert into sys_role_menu values (1761300000000000003, 1761400000000001064);
insert into sys_role_menu values (1761300000000000003, 1761400000000001065);
insert into sys_role_menu values (1761300000000000003, 1761400000000001500);
insert into sys_role_menu values (1761300000000000003, 1761400000000001501);
insert into sys_role_menu values (1761300000000000003, 1761400000000001502);
insert into sys_role_menu values (1761300000000000003, 1761400000000001503);
insert into sys_role_menu values (1761300000000000003, 1761400000000001504);
insert into sys_role_menu values (1761300000000000003, 1761400000000001505);
insert into sys_role_menu values (1761300000000000003, 1761400000000001506);
insert into sys_role_menu values (1761300000000000003, 1761400000000001507);
insert into sys_role_menu values (1761300000000000003, 1761400000000001508);
insert into sys_role_menu values (1761300000000000003, 1761400000000001509);
insert into sys_role_menu values (1761300000000000003, 1761400000000001510);
insert into sys_role_menu values (1761300000000000003, 1761400000000001511);
insert into sys_role_menu values (1761300000000000003, 1761400000000001600);
insert into sys_role_menu values (1761300000000000003, 1761400000000001601);
insert into sys_role_menu values (1761300000000000003, 1761400000000001602);
insert into sys_role_menu values (1761300000000000003, 1761400000000001603);
insert into sys_role_menu values (1761300000000000003, 1761400000000001620);
insert into sys_role_menu values (1761300000000000003, 1761400000000001621);
insert into sys_role_menu values (1761300000000000003, 1761400000000001622);
insert into sys_role_menu values (1761300000000000003, 1761400000000001623);
insert into sys_role_menu values (1761300000000000003, 1761400000000011616);
insert into sys_role_menu values (1761300000000000003, 1761400000000011618);
insert into sys_role_menu values (1761300000000000003, 1761400000000011619);
insert into sys_role_menu values (1761300000000000003, 1761400000000011622);
insert into sys_role_menu values (1761300000000000003, 1761400000000011623);
insert into sys_role_menu values (1761300000000000003, 1761400000000011629);
insert into sys_role_menu values (1761300000000000003, 1761400000000011632);
insert into sys_role_menu values (1761300000000000003, 1761400000000011633);
insert into sys_role_menu values (1761300000000000003, 1761400000000011638);
insert into sys_role_menu values (1761300000000000003, 1761400000000011639);
insert into sys_role_menu values (1761300000000000003, 1761400000000011640);
insert into sys_role_menu values (1761300000000000003, 1761400000000011641);
insert into sys_role_menu values (1761300000000000003, 1761400000000011642);
insert into sys_role_menu values (1761300000000000003, 1761400000000011643);
insert into sys_role_menu values (1761300000000000003, 1761400000000011701);
insert into sys_role_menu values (1761300000000000004, 1761400000000000005);
insert into sys_role_menu values (1761300000000000004, 1761400000000001500);
insert into sys_role_menu values (1761300000000000004, 1761400000000001501);
insert into sys_role_menu values (1761300000000000004, 1761400000000001502);
insert into sys_role_menu values (1761300000000000004, 1761400000000001503);
insert into sys_role_menu values (1761300000000000004, 1761400000000001504);
insert into sys_role_menu values (1761300000000000004, 1761400000000001505);
insert into sys_role_menu values (1761300000000000004, 1761400000000001506);
insert into sys_role_menu values (1761300000000000004, 1761400000000001507);
insert into sys_role_menu values (1761300000000000004, 1761400000000001508);
insert into sys_role_menu values (1761300000000000004, 1761400000000001509);
insert into sys_role_menu values (1761300000000000004, 1761400000000001510);
insert into sys_role_menu values (1761300000000000004, 1761400000000001511);

-- ----------------------------
-- 8、角色和部门关联表  角色1-N部门
-- ----------------------------
create table sys_role_dept (
  role_id   bigint(20) not null comment '角色ID',
  dept_id   bigint(20) not null comment '部门ID',
  primary key(role_id, dept_id)
) engine=innodb comment = '角色和部门关联表';

-- ----------------------------
-- 9、用户与岗位关联表  用户1-N岗位
-- ----------------------------
create table sys_user_post
(
  user_id   bigint(20) not null comment '用户ID',
  post_id   bigint(20) not null comment '岗位ID',
  primary key (user_id, post_id)
) engine=innodb comment = '用户与岗位关联表';

-- ----------------------------
-- 初始化-用户与岗位关联表数据
-- ----------------------------
insert into sys_user_post values (1761100000000000001, 1761200000000000001);

-- ----------------------------
-- 10、操作日志记录
-- ----------------------------
create table sys_oper_log (
  oper_id           bigint(20)      not null                   comment '日志主键',
  title             varchar(50)     default ''                 comment '模块标题',
  business_type     int(2)          default 0                  comment '业务类型（0其它 1新增 2修改 3删除）',
  method            varchar(100)    default ''                 comment '方法名称',
  request_method    varchar(10)     default ''                 comment '请求方式',
  operator_type     int(1)          default 0                  comment '操作类别（0其它 1后台用户 2手机端用户）',
  oper_name         varchar(50)     default ''                 comment '操作人员',
  user_id           bigint(20)      default null               comment '操作用户ID',
  dept_id           bigint(20)      default null               comment '操作部门ID',
  dept_name         varchar(50)     default ''                 comment '部门名称',
  client_key        varchar(32)     default ''                 comment '客户端',
  device_type       varchar(32)     default ''                 comment '设备类型',
  browser           varchar(50)     default ''                 comment '浏览器类型',
  os                varchar(50)     default ''                 comment '操作系统',
  oper_url          varchar(255)    default ''                 comment '请求URL',
  oper_ip           varchar(128)    default ''                 comment '主机地址',
  oper_location     varchar(255)    default ''                 comment '操作地点',
  oper_param        varchar(4000)   default ''                 comment '请求参数',
  json_result       varchar(4000)   default ''                 comment '返回参数',
  status            int(1)          default 0                  comment '操作状态（0正常 1异常）',
  error_msg         varchar(4000)   default ''                 comment '错误消息',
  oper_time         datetime                                   comment '操作时间',
  cost_time         bigint(20)      default 0                  comment '消耗时间',
  primary key (oper_id),
  key idx_sys_oper_log_bt (business_type),
  key idx_sys_oper_log_uid (user_id),
  key idx_sys_oper_log_s  (status),
  key idx_sys_oper_log_ot (oper_time)
) engine=innodb comment = '操作日志记录';


-- ----------------------------
-- 11、字典类型表
-- ----------------------------
create table sys_dict_type
(
  dict_id          bigint(20)      not null                   comment '字典主键',
  dict_name        varchar(100)    default ''                 comment '字典名称',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  create_dept      bigint(20)      default null               comment '创建部门',
  create_by        bigint(20)      default null               comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        bigint(20)      default null               comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_id),
  unique (dict_type)
) engine=innodb comment = '字典类型表';

insert into sys_dict_type values(1761500000000000001, '用户性别', 'sys_user_gender', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '用户性别列表');
insert into sys_dict_type values(1761500000000000002, '菜单状态', 'sys_show_hide', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '菜单状态列表');
insert into sys_dict_type values(1761500000000000003, '系统开关', 'sys_normal_disable', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '系统开关列表');
insert into sys_dict_type values(1761500000000000006, '系统是否', 'sys_yes_no', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '系统是否列表');
insert into sys_dict_type values(1761500000000000007, '通知类型', 'sys_notice_type', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '通知类型列表');
insert into sys_dict_type values(1761500000000000008, '通知状态', 'sys_notice_status', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '通知状态列表');
insert into sys_dict_type values(1761500000000000009, '操作类型', 'sys_oper_type', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '操作类型列表');
insert into sys_dict_type values(1761500000000000010, '系统状态', 'sys_common_status', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '登录状态列表');
insert into sys_dict_type values(1761500000000000011, '授权类型', 'sys_grant_type', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '认证授权类型');
insert into sys_dict_type values(1761500000000000012, '设备类型', 'sys_device_type', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '客户端设备类型');
insert into sys_dict_type values(1761500000000000013, '业务状态', 'wf_business_status', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '业务状态列表');
insert into sys_dict_type values(1761500000000000014, '表单类型', 'wf_form_type', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '表单类型列表');
insert into sys_dict_type values(1761500000000000015, '任务状态', 'wf_task_status', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '任务状态');

-- ----------------------------
-- 12、字典数据表
-- ----------------------------
create table sys_dict_data
(
  dict_code        bigint(20)      not null                   comment '字典编码',
  dict_sort        int(4)          default 0                  comment '字典排序',
  dict_label       varchar(100)    default ''                 comment '字典标签',
  dict_value       varchar(100)    default ''                 comment '字典键值',
  dict_type        varchar(100)    default ''                 comment '字典类型',
  css_class        varchar(100)    default null               comment '样式属性（其他样式扩展）',
  list_class       varchar(100)    default null               comment '表格回显样式',
  is_default       char(1)         default 'N'                comment '是否默认（Y是 N否）',
  create_dept      bigint(20)      default null               comment '创建部门',
  create_by        bigint(20)      default null               comment '创建者',
  create_time      datetime                                   comment '创建时间',
  update_by        bigint(20)      default null               comment '更新者',
  update_time      datetime                                   comment '更新时间',
  remark           varchar(500)    default null               comment '备注',
  primary key (dict_code),
  key idx_sys_dict_data_type (dict_type)
) engine=innodb comment = '字典数据表';

insert into sys_dict_data values(1761600000000000001, 1, '男', '0', 'sys_user_gender', '', '', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '性别男');
insert into sys_dict_data values(1761600000000000002, 2, '女', '1', 'sys_user_gender', '', '', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '性别女');
insert into sys_dict_data values(1761600000000000003, 3, '未知', '2', 'sys_user_gender', '', '', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '性别未知');
insert into sys_dict_data values(1761600000000000004, 1, '显示', '0', 'sys_show_hide', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '显示菜单');
insert into sys_dict_data values(1761600000000000005, 2, '隐藏', '1', 'sys_show_hide', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '隐藏菜单');
insert into sys_dict_data values(1761600000000000006, 1, '正常', '0', 'sys_normal_disable', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '正常状态');
insert into sys_dict_data values(1761600000000000007, 2, '停用', '1', 'sys_normal_disable', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '停用状态');
insert into sys_dict_data values(1761600000000000012, 1, '是', 'Y', 'sys_yes_no', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '系统默认是');
insert into sys_dict_data values(1761600000000000013, 2, '否', 'N', 'sys_yes_no', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '系统默认否');
insert into sys_dict_data values(1761600000000000014, 1, '通知', '1', 'sys_notice_type', '', 'warning', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '通知');
insert into sys_dict_data values(1761600000000000015, 2, '公告', '2', 'sys_notice_type', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '公告');
insert into sys_dict_data values(1761600000000000016, 1, '正常', '0', 'sys_notice_status', '', 'primary', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '正常状态');
insert into sys_dict_data values(1761600000000000017, 2, '关闭', '1', 'sys_notice_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '关闭状态');
insert into sys_dict_data values(1761600000000000029, 99, '其他', '0', 'sys_oper_type', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '其他操作');
insert into sys_dict_data values(1761600000000000018, 1, '新增', '1', 'sys_oper_type', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '新增操作');
insert into sys_dict_data values(1761600000000000019, 2, '修改', '2', 'sys_oper_type', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '修改操作');
insert into sys_dict_data values(1761600000000000020, 3, '删除', '3', 'sys_oper_type', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '删除操作');
insert into sys_dict_data values(1761600000000000021, 4, '授权', '4', 'sys_oper_type', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '授权操作');
insert into sys_dict_data values(1761600000000000022, 5, '导出', '5', 'sys_oper_type', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '导出操作');
insert into sys_dict_data values(1761600000000000023, 6, '导入', '6', 'sys_oper_type', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '导入操作');
insert into sys_dict_data values(1761600000000000024, 7, '强退', '7', 'sys_oper_type', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '强退操作');
insert into sys_dict_data values(1761600000000000025, 8, '生成代码', '8', 'sys_oper_type', '', 'warning', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '生成操作');
insert into sys_dict_data values(1761600000000000026, 9, '清空数据', '9', 'sys_oper_type', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '清空操作');
insert into sys_dict_data values(1761600000000000027, 1, '成功', '0', 'sys_common_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '正常状态');
insert into sys_dict_data values(1761600000000000028, 2, '失败', '1', 'sys_common_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '停用状态');
insert into sys_dict_data values(1761600000000000030, 0, '密码认证', 'password', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '密码认证');
insert into sys_dict_data values(1761600000000000031, 0, '短信认证', 'sms', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '短信认证');
insert into sys_dict_data values(1761600000000000032, 0, '邮件认证', 'email', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '邮件认证');
insert into sys_dict_data values(1761600000000000033, 0, '小程序认证', 'xcx', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '小程序认证');
insert into sys_dict_data values(1761600000000000034, 0, '三方登录认证', 'social', 'sys_grant_type', 'el-check-tag', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '三方登录认证');
insert into sys_dict_data values(1761600000000000035, 0, 'PC', 'pc', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, 'PC');
insert into sys_dict_data values(1761600000000000036, 0, '安卓', 'android', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '安卓');
insert into sys_dict_data values(1761600000000000037, 0, 'iOS', 'ios', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, 'iOS');
insert into sys_dict_data values(1761600000000000038, 0, '小程序', 'xcx', 'sys_device_type', '', 'default', 'N', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '小程序');
insert into sys_dict_data values(1761600000000000039, 1, '已撤销', 'cancel', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已撤销');
insert into sys_dict_data values(1761600000000000040, 2, '草稿', 'draft', 'wf_business_status', '', 'info', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '草稿');
insert into sys_dict_data values(1761600000000000041, 3, '待审核', 'waiting', 'wf_business_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '待审核');
insert into sys_dict_data values(1761600000000000042, 4, '已完成', 'finish', 'wf_business_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已完成');
insert into sys_dict_data values(1761600000000000043, 5, '已作废', 'invalid', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已作废');
insert into sys_dict_data values(1761600000000000044, 6, '已退回', 'back', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已退回');
insert into sys_dict_data values(1761600000000000045, 7, '已终止', 'termination', 'wf_business_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '已终止');
insert into sys_dict_data values(1761600000000000046, 1, '自定义表单', 'static', 'wf_form_type', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '自定义表单');
insert into sys_dict_data values(1761600000000000047, 2, '动态表单', 'dynamic', 'wf_form_type', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '动态表单');
insert into sys_dict_data values(1761600000000000048, 1, '撤销', 'cancel', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '撤销');
insert into sys_dict_data values(1761600000000000049, 2, '通过', 'pass', 'wf_task_status', '', 'success', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '通过');
insert into sys_dict_data values(1761600000000000050, 3, '待审核', 'waiting', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '待审核');
insert into sys_dict_data values(1761600000000000051, 4, '作废', 'invalid', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '作废');
insert into sys_dict_data values(1761600000000000052, 5, '退回', 'back', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '退回');
insert into sys_dict_data values(1761600000000000053, 6, '终止', 'termination', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '终止');
insert into sys_dict_data values(1761600000000000054, 7, '转办', 'transfer', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '转办');
insert into sys_dict_data values(1761600000000000055, 8, '委托', 'depute', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '委托');
insert into sys_dict_data values(1761600000000000056, 9, '抄送', 'copy', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '抄送');
insert into sys_dict_data values(1761600000000000057, 10, '加签', 'sign', 'wf_task_status', '', 'primary', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '加签');
insert into sys_dict_data values(1761600000000000058, 11, '减签', 'sign_off', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '减签');
insert into sys_dict_data values(1761600000000000059, 11, '超时', 'timeout', 'wf_task_status', '', 'danger', 'N', 1761000000000000103, 1761100000000000001, sysdate(), NULL, NULL, '超时');


-- ----------------------------
-- 13、参数配置表
-- ----------------------------
create table sys_config (
  config_id         bigint(20)      not null                   comment '参数主键',
  config_name       varchar(100)    default ''                 comment '参数名称',
  config_key        varchar(100)    default ''                 comment '参数键名',
  config_value      varchar(500)    default ''                 comment '参数键值',
  config_type       char(1)         default 'N'                comment '系统内置（Y是 N否）',
  create_dept       bigint(20)      default null               comment '创建部门',
  create_by         bigint(20)      default null               comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         bigint(20)      default null               comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(500)    default null               comment '备注',
  primary key (config_id)
) engine=innodb comment = '参数配置表';

insert into sys_config values(1761700000000000001, '用户管理-账号初始密码', 'sys.user.initPassword', '123456', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '初始化密码 123456');
insert into sys_config values(1761700000000000002, '账号自助-是否开启用户注册功能', 'sys.account.registerUser', 'false', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '是否开启注册用户功能（true开启，false关闭）');
insert into sys_config values(1761700000000000003, 'OSS预览列表资源开关', 'sys.oss.previewListResource', 'true', 'Y', 1761000000000000103, 1761100000000000001, sysdate(), null, null, 'true:开启, false:关闭');


-- ----------------------------
-- 14、系统访问记录
-- ----------------------------
create table sys_login_info (
  info_id        bigint(20)     not null                  comment '访问ID',
  user_name      varchar(50)    default ''                comment '用户账号',
  client_key     varchar(32)    default ''                comment '客户端',
  device_type    varchar(32)    default ''                comment '设备类型',
  ipaddr         varchar(128)   default ''                comment '登录IP地址',
  login_location varchar(255)   default ''                comment '登录地点',
  browser        varchar(50)    default ''                comment '浏览器类型',
  os             varchar(50)    default ''                comment '操作系统',
  status         char(1)        default '0'               comment '登录状态（0正常 1异常）',
  msg            varchar(255)   default ''                comment '提示消息',
  login_time     datetime                                 comment '访问时间',
  primary key (info_id),
  key idx_sys_login_info_s  (status),
  key idx_sys_login_info_lt (login_time)
) engine=innodb comment = '系统访问记录';


-- ----------------------------
-- 17、通知公告表
-- ----------------------------
create table sys_notice (
  notice_id         bigint(20)      not null                   comment '公告ID',
  notice_title      varchar(50)     not null                   comment '公告标题',
  notice_type       char(1)         not null                   comment '公告类型（1通知 2公告）',
  notice_content    longblob        default null               comment '公告内容',
  status            char(1)         default '0'                comment '公告状态（0正常 1关闭）',
  create_dept       bigint(20)      default null               comment '创建部门',
  create_by         bigint(20)      default null               comment '创建者',
  create_time       datetime                                   comment '创建时间',
  update_by         bigint(20)      default null               comment '更新者',
  update_time       datetime                                   comment '更新时间',
  remark            varchar(255)    default null               comment '备注',
  primary key (notice_id)
) engine=innodb comment = '通知公告表';

-- ----------------------------
-- 初始化-公告信息表数据
-- ----------------------------
insert into sys_notice values(1761800000000000001, '温馨提醒：2018-07-01 新版本发布啦', '2', '新版本内容', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '管理员');
insert into sys_notice values(1761800000000000002, '维护通知：2018-07-01 系统凌晨维护', '1', '维护内容', '0', 1761000000000000103, 1761100000000000001, sysdate(), null, null, '管理员');


-- ----------------------------
-- 18、消息记录表
-- ----------------------------
create table sys_message (
    message_id        bigint(20)      not null                   comment '消息ID',
    category          varchar(20)     not null                   comment '消息分组(system/notice)',
    type              varchar(20)     not null                   comment '消息类型',
    source            varchar(20)     not null                   comment '消息来源',
    title             varchar(100)    default ''                 comment '标题',
    message           varchar(500)    default ''                 comment '摘要消息',
    content           longtext                                   comment '详细内容',
    data_json         longtext                                   comment '扩展数据JSON',
    path              varchar(500)    default null               comment '前端跳转路径',
    send_user_ids     varchar(2000)   not null default '0'       comment '目标用户ID串，0表示全局',
    create_dept       bigint(20)      default null               comment '创建部门',
    create_by         bigint(20)      default null               comment '创建者',
    create_time       datetime                                   comment '创建时间',
    update_by         bigint(20)      default null               comment '更新者',
    update_time       datetime                                   comment '更新时间',
    primary key (message_id),
    key idx_sys_message_category_time (category, create_time)
) engine=innodb comment = '消息记录表';


-- ----------------------------
-- ----------------------------
-- OSS对象存储表
-- ----------------------------
create table sys_oss (
  oss_id          bigint(20)   not null                   comment '对象存储主键',
  file_name       varchar(255) not null default ''        comment '文件名',
  original_name   varchar(255) not null default ''        comment '原名',
  file_suffix     varchar(10)  not null default ''        comment '文件后缀名',
  url             varchar(500) not null                   comment 'URL地址',
  ext1            text                  default null      comment '扩展字段',
  create_dept     bigint(20)            default null      comment '创建部门',
  create_time     datetime              default null      comment '创建时间',
  create_by       bigint(20)            default null      comment '上传人',
  update_time     datetime              default null      comment '更新时间',
  update_by       bigint(20)            default null      comment '更新人',
  service         varchar(20)  not null default 'minio'   comment '服务商',
  primary key (oss_id)
) engine=innodb comment ='OSS对象存储表';

-- ----------------------------
-- OSS对象存储动态配置表
-- ----------------------------
create table sys_oss_config (
  oss_config_id   bigint(20)    not null                  comment '主键',
  config_key      varchar(20)   not null  default ''      comment '配置key',
  access_key      varchar(255)            default ''      comment 'accessKey',
  secret_key      varchar(255)            default ''      comment '秘钥',
  bucket_name     varchar(255)            default ''      comment '桶名称',
  prefix          varchar(255)            default ''      comment '前缀',
  endpoint        varchar(255)            default ''      comment '访问站点',
  domain_url      varchar(255)            default ''      comment '自定义域名',
  is_https        char(1)                 default 'N'     comment '是否https（Y=是,N=否）',
  region          varchar(255)            default ''      comment '域',
  access_policy   char(1)       not null  default '1'     comment '桶权限类型(0=private 1=public 2=custom)',
  status          char(1)                 default 'N'     comment '是否默认（Y=是,N=否）',
  ext1            varchar(255)            default ''      comment '扩展字段',
  create_dept     bigint(20)              default null    comment '创建部门',
  create_by       bigint(20)              default null    comment '创建者',
  create_time     datetime                default null    comment '创建时间',
  update_by       bigint(20)              default null    comment '更新者',
  update_time     datetime                default null    comment '更新时间',
  remark          varchar(500)            default null    comment '备注',
  primary key (oss_config_id)
) engine=innodb comment='对象存储配置表';

insert into sys_oss_config values (1761900000000000001, 'minio', 'ruoyi', 'ruoyi123', 'ruoyi', '', '127.0.0.1:9000', '', 'N', '', '1', 'Y', '', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate(), null);
insert into sys_oss_config values (1761900000000000002, 'qiniu', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 's3-cn-north-1.qiniucs.com', '', 'N', '', '1', 'N', '', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate(), null);
insert into sys_oss_config values (1761900000000000003, 'aliyun', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi', '', 'oss-cn-beijing.aliyuncs.com', '', 'N', '', '1', 'N', '', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate(), null);
insert into sys_oss_config values (1761900000000000004, 'qcloud', 'XXXXXXXXXXXXXXX', 'XXXXXXXXXXXXXXX', 'ruoyi-1240000000', '', 'cos.ap-beijing.myqcloud.com', '', 'N', 'ap-beijing', '1', 'N', '', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate(), null);
insert into sys_oss_config values (1761900000000000005, 'image', 'ruoyi', 'ruoyi123', 'ruoyi', 'image', '127.0.0.1:9000', '', 'N', '', '1', 'N', '', 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate(), null);

-- ----------------------------
-- 系统授权表
-- ----------------------------
create table sys_client (
    id                  bigint(20)    not null            comment 'id',
    client_id           varchar(64)   default null        comment '客户端id',
    client_key          varchar(32)   default null        comment '客户端key',
    client_secret       varchar(255)  default null        comment '客户端秘钥',
    grant_type          varchar(255)  default null        comment '授权类型',
    device_type         varchar(32)   default null        comment '设备类型',
    access_path         varchar(1024) default null        comment '允许访问路径',
    ip_whitelist        varchar(1024) default null        comment 'IP白名单',
    active_timeout      int(11)       default 1800        comment 'token活跃超时时间',
    timeout             int(11)       default 604800      comment 'token固定超时',
    status              char(1)       default '0'         comment '状态（0正常 1停用）',
    del_flag            char(1)       default '0'         comment '删除标志（0代表存在 1代表删除）',
    create_dept         bigint(20)    default null        comment '创建部门',
    create_by           bigint(20)    default null        comment '创建者',
    create_time         datetime      default null        comment '创建时间',
    update_by           bigint(20)    default null        comment '更新者',
    update_time         datetime      default null        comment '更新时间',
    primary key (id)
) engine=innodb comment='系统授权表';

insert into sys_client values (1762000000000000001, 'e5cd7e4891bf95d1d19206ce24a7b32e', 'pc', 'pc123', 'password,social', 'pc', null, null, 1800, 604800, 0, 0, 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate());
insert into sys_client values (1762000000000000002, '428a8310cd442757ae699df5d894f051', 'app', 'app123', 'password,sms,social', 'android', null, null, 1800, 604800, 0, 0, 1761000000000000103, 1761100000000000001, sysdate(), 1761100000000000001, sysdate());

-- -----------------------------------------------------------------------------
-- 多租户最终结构（已合并自 update_6.0.0-tenant.sql）。
-- 上方基础数据先按原始列顺序写入；以下新增列均带默认租户，执行完本文件后所有
-- 租户隔离表均处于最终结构，无需再执行升级 SQL。
-- -----------------------------------------------------------------------------

ALTER TABLE sys_social
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER id,
    ADD KEY idx_sys_social_tenant_id (tenant_id);

ALTER TABLE sys_dept
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER dept_id,
    ADD KEY idx_sys_dept_tenant_id (tenant_id);

ALTER TABLE sys_user
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER user_id,
    ADD UNIQUE KEY uk_sys_user_tenant_global_user (tenant_id, global_user_id);

ALTER TABLE sys_post
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER post_id,
    ADD KEY idx_sys_post_tenant_id (tenant_id);

ALTER TABLE sys_role
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER role_id,
    ADD KEY idx_sys_role_tenant_role_key (tenant_id, role_key);

ALTER TABLE sys_oper_log
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER oper_id,
    ADD KEY idx_sys_oper_log_tenant_time (tenant_id, oper_time);

ALTER TABLE sys_config
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER config_id,
    ADD KEY idx_sys_config_tenant_key (tenant_id, config_key);

ALTER TABLE sys_login_info
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER info_id,
    ADD KEY idx_sys_login_info_tenant_time (tenant_id, login_time);

ALTER TABLE sys_notice
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER notice_id,
    ADD KEY idx_sys_notice_tenant_id (tenant_id);

ALTER TABLE sys_message
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER message_id,
    ADD KEY idx_sys_message_tenant_time (tenant_id, create_time);

ALTER TABLE sys_oss
    ADD COLUMN tenant_id varchar(20) NOT NULL DEFAULT '000000' COMMENT '租户编号' AFTER oss_id,
    ADD KEY idx_sys_oss_tenant_id (tenant_id);

-- -----------------------------------------------------------------------------
-- 租户与套餐主数据。
-- -----------------------------------------------------------------------------

CREATE TABLE sys_tenant (
    id                bigint(20)      NOT NULL COMMENT '主键',
    tenant_id         varchar(20)     NOT NULL COMMENT '租户编号',
    contact_user_name varchar(50)     NOT NULL COMMENT '联系人',
    contact_phone     varchar(20)     NOT NULL COMMENT '联系电话',
    company_name      varchar(100)    NOT NULL COMMENT '企业名称',
    license_number    varchar(100)    DEFAULT NULL COMMENT '统一社会信用代码',
    address           varchar(255)    DEFAULT NULL COMMENT '地址',
    domain            varchar(255)    DEFAULT NULL COMMENT '域名',
    intro             varchar(1000)   DEFAULT NULL COMMENT '企业简介',
    package_id        bigint(20)      DEFAULT NULL COMMENT '租户套餐编号',
    expire_time       datetime        DEFAULT NULL COMMENT '过期时间',
    account_count     bigint(20)      NOT NULL DEFAULT -1 COMMENT '用户数量上限，-1表示不限制',
    status            char(1)         NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag          char(1)         NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    create_dept       bigint(20)      DEFAULT NULL COMMENT '创建部门',
    create_by         bigint(20)      DEFAULT NULL COMMENT '创建者',
    create_time       datetime        DEFAULT NULL COMMENT '创建时间',
    update_by         bigint(20)      DEFAULT NULL COMMENT '更新者',
    update_time       datetime        DEFAULT NULL COMMENT '更新时间',
    remark            varchar(500)    DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_tenant_tenant_id (tenant_id),
    KEY idx_sys_tenant_status (status)
) ENGINE=InnoDB COMMENT='租户表';

CREATE TABLE sys_tenant_package (
    package_id          bigint(20)    NOT NULL COMMENT '套餐主键',
    package_name        varchar(100)  NOT NULL COMMENT '套餐名称',
    menu_ids            varchar(4000) DEFAULT '' COMMENT '关联菜单ID，逗号分隔',
    menu_check_strictly tinyint(1)    NOT NULL DEFAULT 1 COMMENT '菜单树是否父子联动',
    status              char(1)       NOT NULL DEFAULT '0' COMMENT '状态（0正常 1停用）',
    del_flag            char(1)       NOT NULL DEFAULT '0' COMMENT '删除标志（0存在 1删除）',
    create_dept         bigint(20)    DEFAULT NULL COMMENT '创建部门',
    create_by           bigint(20)    DEFAULT NULL COMMENT '创建者',
    create_time         datetime      DEFAULT NULL COMMENT '创建时间',
    update_by           bigint(20)    DEFAULT NULL COMMENT '更新者',
    update_time         datetime      DEFAULT NULL COMMENT '更新时间',
    remark              varchar(500)  DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (package_id),
    KEY idx_sys_tenant_package_status (status)
) ENGINE=InnoDB COMMENT='租户套餐表';

-- 默认套餐仅包含基础系统菜单；下方新增的租户管理菜单仅授予平台超级管理员。
SET SESSION group_concat_max_len = 8192;
INSERT INTO sys_tenant_package
    (package_id, package_name, menu_ids, menu_check_strictly, status, del_flag,
     create_dept, create_by, create_time, remark)
SELECT 1762000000000000001,
       '默认套餐',
       COALESCE(GROUP_CONCAT(CAST(menu_id AS CHAR) ORDER BY menu_id SEPARATOR ','), ''),
       1, '0', '0', 1761000000000000103, 1761100000000000001, SYSDATE(), '默认租户套餐'
FROM sys_menu;

INSERT INTO sys_tenant
    (id, tenant_id, contact_user_name, contact_phone, company_name, package_id,
     account_count, status, del_flag, create_dept, create_by, create_time, remark)
VALUES
    (1762000000000000002, '000000', '平台管理员', '15888888888', '默认管理租户',
     1762000000000000001, -1, '0', '0', 1761000000000000103,
     1761100000000000001, SYSDATE(), '系统初始化的默认租户');

-- 平台租户管理菜单与按钮权限。
INSERT INTO sys_menu
    (menu_id, menu_name, parent_id, order_num, path, component, query_param, is_frame, is_cache,
     menu_type, visible, status, perms, icon, active_menu, ext, create_dept, create_by, create_time,
     update_by, update_time, remark)
VALUES
    (1761400000000001700, '租户管理', 1761400000000000001, 12, 'tenant', 'system/tenant/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenant:list', 'company', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '平台租户管理菜单'),
    (1761400000000001701, '租户查询', 1761400000000001700, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001702, '租户新增', 1761400000000001700, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001703, '租户修改', 1761400000000001700, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001704, '租户删除', 1761400000000001700, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001705, '租户导出', 1761400000000001700, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenant:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001710, '租户套餐', 1761400000000000001, 13, 'tenant-package', 'system/tenant/package/index', '', 'N', 'Y', 'C', '0', '0', 'system:tenantPackage:list', 'price-tag', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '平台租户套餐菜单'),
    (1761400000000001711, '套餐查询', 1761400000000001710, 1, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:query', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001712, '套餐新增', 1761400000000001710, 2, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:add', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001713, '套餐修改', 1761400000000001710, 3, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:edit', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001714, '套餐删除', 1761400000000001710, 4, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:remove', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, ''),
    (1761400000000001715, '套餐导出', 1761400000000001710, 5, '', '', '', 'N', 'Y', 'F', '0', '0', 'system:tenantPackage:export', '#', '', '', 1761000000000000103, 1761100000000000001, SYSDATE(), NULL, NULL, '');

-- 默认管理租户超级管理员拥有平台级租户管理权限。
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (1761300000000000001, 1761400000000001700),
    (1761300000000000001, 1761400000000001701),
    (1761300000000000001, 1761400000000001702),
    (1761300000000000001, 1761400000000001703),
    (1761300000000000001, 1761400000000001704),
    (1761300000000000001, 1761400000000001705),
    (1761300000000000001, 1761400000000001710),
    (1761300000000000001, 1761400000000001711),
    (1761300000000000001, 1761400000000001712),
    (1761300000000000001, 1761400000000001713),
    (1761300000000000001, 1761400000000001714),
    (1761300000000000001, 1761400000000001715);


-- for AT mode you must to init this sql for you business database. the seata server not need it.
CREATE TABLE IF NOT EXISTS undo_log
(
    branch_id     BIGINT(20)   NOT NULL COMMENT 'branch transaction id',
    xid           VARCHAR(100) NOT NULL COMMENT 'global transaction id',
    context       VARCHAR(128) NOT NULL COMMENT 'undo_log context,such as serialization',
    rollback_info LONGBLOB     NOT NULL COMMENT 'rollback info',
    log_status    INT(11)      NOT NULL COMMENT '0:normal status,1:defense status',
    log_created   DATETIME(6)  NOT NULL COMMENT 'create datetime',
    log_modified  DATETIME(6)  NOT NULL COMMENT 'modify datetime',
    UNIQUE KEY ux_undo_log (xid, branch_id)
) ENGINE = InnoDB COMMENT ='AT transaction mode undo table';
