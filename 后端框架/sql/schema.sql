-- ============================================================
-- 企程（OrgFlow）数据库建表脚本
-- 数据库：orgflow    字符集：utf8mb4    排序规则：utf8mb4_unicode_ci
-- 说明：本脚本可重复执行（IF NOT EXISTS），只建结构不插数据；
--       演示数据请执行同目录下的 init_data.sql
--
-- 执行方式（命令行）：
--   先建库：
--     "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 -e "CREATE DATABASE IF NOT EXISTS orgflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
--   再建表：
--     "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 orgflow < schema.sql
-- 执行方式（Navicat）：打开 orgflow -> 新建查询 -> 粘贴本文件内容 -> 运行
-- ============================================================

USE `orgflow`;

-- ------------------------------------------------------------
-- 1. 部门表：支持多级部门（parent_id = 0 表示顶级）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `department` (
  `id`          int NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `name`        varchar(50) NOT NULL COMMENT '部门名称',
  `parent_id`   int DEFAULT '0' COMMENT '父级部门ID (0表示顶级部门)',
  `sort_order`  int DEFAULT '0' COMMENT '同级排序序号',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- ------------------------------------------------------------
-- 2. 员工表：员工与部门是多对一关系
--    status: 1 在职  0 离职
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `employee` (
  `id`            int NOT NULL AUTO_INCREMENT COMMENT '员工ID',
  `name`          varchar(20) NOT NULL COMMENT '姓名',
  `department_id` int DEFAULT NULL COMMENT '所属部门ID',
  `phone`         varchar(11) DEFAULT NULL COMMENT '手机号',
  `email`         varchar(50) DEFAULT NULL COMMENT '邮箱',
  `position`      varchar(20) DEFAULT NULL COMMENT '职位',
  `entry_date`    date DEFAULT NULL COMMENT '入职日期',
  `status`        tinyint DEFAULT '1' COMMENT '状态: 1在职, 0离职',
  `create_time`   datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_dept_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工通讯录表';

-- ------------------------------------------------------------
-- 3. 用户账号表：与员工表通过 employee_id 关联，实现权限与信息解耦
--    role:   1 管理员  2 普通员工
--    status: 1 启用    0 禁用
--    password 存 BCrypt 密文，绝不存明文
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `user` (
  `id`          int NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username`    varchar(30) NOT NULL COMMENT '登录账号',
  `password`    varchar(100) NOT NULL COMMENT '登录密码（BCrypt 密文）',
  `employee_id` int DEFAULT NULL COMMENT '关联的员工ID',
  `role`        tinyint DEFAULT '2' COMMENT '角色: 1管理员, 2普通员工',
  `status`      tinyint DEFAULT '1' COMMENT '状态: 1启用, 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_employee_id` (`employee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户账号表';

-- ------------------------------------------------------------
-- 4. 日程公告表
--    type:      1 公司公告  2 个人/部门日程
--    create_by: 发布人的 user 表 id（不是 employee id）
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `schedule` (
  `id`          int NOT NULL AUTO_INCREMENT COMMENT '日程ID',
  `title`       varchar(100) NOT NULL COMMENT '标题',
  `content`     text COMMENT '内容详情',
  `type`        tinyint DEFAULT '1' COMMENT '类型: 1公司公告, 2个人/部门日程',
  `start_time`  datetime DEFAULT NULL COMMENT '开始时间',
  `end_time`    datetime DEFAULT NULL COMMENT '结束时间',
  `create_by`   int DEFAULT NULL COMMENT '发布人ID（user 表 id）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_type_start` (`type`, `start_time`),
  KEY `idx_create_by` (`create_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日程公告表';

-- ------------------------------------------------------------
-- 校验：应输出 4 张表
-- ------------------------------------------------------------
SELECT TABLE_NAME AS 表名, TABLE_COMMENT AS 说明
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'orgflow'
ORDER BY TABLE_NAME;
