-- ============================================================
-- 企程（OrgFlow）演示数据初始化脚本
-- 数据库：orgflow   字符集：utf8mb4
-- 说明：本脚本可重复执行，每次执行都会把演示数据重置为初始状态
-- 执行方式（命令行）：
--   "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p123456 orgflow < init_data.sql
-- 执行方式（Navicat）：打开 orgflow -> 新建查询 -> 粘贴本文件内容 -> 运行
-- ============================================================

USE `orgflow`;

-- 关闭外键检查（本项目未建外键，保险起见）
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 清空旧数据并重置自增 ----------
TRUNCATE TABLE `schedule`;
TRUNCATE TABLE `user`;
TRUNCATE TABLE `employee`;
TRUNCATE TABLE `department`;

-- ---------- 1. 部门 ----------
INSERT INTO `department` (`id`, `name`, `parent_id`, `sort_order`) VALUES
  (1, '总经办',   0, 1),
  (2, '技术部',   0, 2),
  (3, '市场部',   0, 3),
  (4, '财务部',   0, 4),
  (5, '人力资源部', 0, 5),
  (6, '研发一组', 2, 1),
  (7, '研发二组', 2, 2);

-- ---------- 2. 员工 ----------
INSERT INTO `employee` (`id`, `name`, `department_id`, `phone`, `email`, `position`, `entry_date`, `status`) VALUES
  (1,  '张伟',  1, '13800000001', 'zhangwei@orgflow.com',  '总经理',     '2015-03-01', 1),
  (2,  '李娜',  1, '13800000002', 'lina@orgflow.com',      '总经理助理', '2017-06-15', 1),
  (3,  '王强',  2, '13800000003', 'wangqiang@orgflow.com', '技术总监',   '2016-09-01', 1),
  (4,  '张三',  2, '13800000004', 'zhangsan@orgflow.com',  'Java开发工程师', '2022-07-01', 1),
  (5,  '李四',  6, '13800000005', 'lisi@orgflow.com',      '前端开发工程师', '2023-03-20', 1),
  (6,  '王五',  7, '13800000006', 'wangwu@orgflow.com',    '测试工程师', '2023-08-10', 1),
  (7,  '赵敏',  3, '13800000007', 'zhaomin@orgflow.com',   '市场经理',   '2018-04-02', 1),
  (8,  '孙俪',  3, '13800000008', 'sunli@orgflow.com',     '市场专员',   '2021-11-08', 1),
  (9,  '周杰',  4, '13800000009', 'zhoujie@orgflow.com',   '财务经理',   '2017-01-09', 1),
  (10, '吴敏',  5, '13800000010', 'wumin@orgflow.com',     '人事经理',   '2019-05-06', 1),
  (11, '郑娜',  5, '13800000011', 'zhengna@orgflow.com',   '人事专员',   '2023-02-13', 1),
  (12, '陈刚',  2, '13800000012', 'chengang@orgflow.com',  '架构师',     '2019-10-21', 0);

-- ---------- 3. 用户账号（密码均为 123456，BCrypt 密文） ----------
-- 角色：1管理员 2普通员工  状态：1启用 0禁用
INSERT INTO `user` (`id`, `username`, `password`, `employee_id`, `role`, `status`) VALUES
  (1, 'admin',    '$2a$10$Wz8pgaMwqBfPM4qyKm/QPOp5eYNH5nmIxFI1/Z7EhXwU/j791UI6C', 1, 1, 1),
  (2, 'zhangsan', '$2a$10$Wz8pgaMwqBfPM4qyKm/QPOp5eYNH5nmIxFI1/Z7EhXwU/j791UI6C', 4, 2, 1),
  (3, 'lisi',     '$2a$10$Wz8pgaMwqBfPM4qyKm/QPOp5eYNH5nmIxFI1/Z7EhXwU/j791UI6C', 5, 2, 1),
  (4, 'wangqiang','$2a$10$Wz8pgaMwqBfPM4qyKm/QPOp5eYNH5nmIxFI1/Z7EhXwU/j791UI6C', 3, 2, 1),
  (5, 'zhaomin',  '$2a$10$Wz8pgaMwqBfPM4qyKm/QPOp5eYNH5nmIxFI1/Z7EhXwU/j791UI6C', 7, 2, 1);

-- ---------- 4. 日程与公告 ----------
-- 类型：1公司公告 2个人/部门日程   create_by 存 user 表 id
INSERT INTO `schedule` (`title`, `content`, `type`, `start_time`, `end_time`, `create_by`) VALUES
  ('关于 2026 年国庆节放假安排的通知',
   '公司全体员工：根据国家法定节假日安排，2026 年国庆节放假时间为 10 月 1 日至 10 月 7 日，共 7 天。10 月 8 日（周四）正常上班，请各部门提前做好工作交接。',
   1, '2026-10-01 00:00:00', '2026-10-07 23:59:59', 1),
  ('公司年度总结大会',
   '定于 11 月 20 日下午 14:00 在一号会议室召开年度总结大会，请各部门负责人准备 10 分钟的汇报材料。',
   1, '2026-11-20 14:00:00', '2026-11-20 17:00:00', 1),
  ('技术部周例会',
   '技术部全体成员参加，同步本周迭代进度与下周排期。',
   2, '2026-10-13 10:00:00', '2026-10-13 11:30:00', 2),
  ('研发一组需求评审',
   '评审通讯录模块二期需求，产品、开发、测试共同参加。',
   2, '2026-10-14 15:00:00', '2026-10-14 16:30:00', 2),
  ('市场部月度复盘',
   '复盘本月市场活动效果，制定下月投放计划。',
   2, '2026-10-15 09:30:00', '2026-10-15 11:00:00', 5);

SET FOREIGN_KEY_CHECKS = 1;

-- ---------- 校验 ----------
SELECT '部门' AS 表名, COUNT(*) AS 记录数 FROM `department`
UNION ALL SELECT '员工', COUNT(*) FROM `employee`
UNION ALL SELECT '用户', COUNT(*) FROM `user`
UNION ALL SELECT '日程', COUNT(*) FROM `schedule`;
