<div align="center">

# 企程 OrgFlow

**公司/部门通讯录与日程管理系统**

基于 Spring Boot 4 + Vue 3 的前后端分离管理系统 · 毕业设计作品

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-6DB33F?logo=springboot)
![MyBatis-Plus](https://img.shields.io/badge/MyBatis--Plus-3.5.17-blue)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?logo=mysql)
![Vue](https://img.shields.io/badge/Vue-3.5-4FC08D?logo=vuedotjs)
![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14-409EFF)
![Vite](https://img.shields.io/badge/Vite-7.3-646CFF?logo=vite)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

</div>

---

## 📖 项目简介

「企程（OrgFlow）」是一个面向企业内网场景的**部门通讯录 + 日程管理**系统，用于解决两个日常痛点：

- **找人难**：组织架构、员工联系方式散落在各处，新人不知道谁在哪个部门、找谁办事
- **通知与日程分散**：公司公告靠群消息刷屏，个人/部门日程没有统一入口，容易漏掉

系统采用**前后端分离**架构：后端提供 RESTful 接口并做统一鉴权，前端负责交互与展示。支持**管理员 / 普通员工**两种角色，权限控制细化到接口与数据归属。

---

## 📸 界面截图

> 把截图文件放到 `docs/screenshots/` 目录下（文件名与下方一致），README 里的图片就会自动显示。

### 登录页

![登录页](docs/screenshots/01-login.png)

### 首页（工作台）

![首页](docs/screenshots/02-dashboard.png)

### 部门管理（树形组织架构）

![部门管理](docs/screenshots/03-department.png)

### 员工通讯录

![员工通讯录](docs/screenshots/04-employee.png)

### 日程与公告

![日程管理](docs/screenshots/05-schedule.png)

### 个人中心

![个人中心](docs/screenshots/06-profile.png)

### 用户账号管理（仅管理员可见）

![用户管理](docs/screenshots/07-user.png)

---

## 👤 演示账号

| 账号 | 密码 | 角色 | 可使用的功能 |
| :--- | :--- | :--- | :--- |
| `admin` | `123456` | 管理员 | 全部功能：部门增删改、员工增删改、发布公司公告、用户账号管理 |
| `zhangsan` | `123456` | 普通员工 | 查看通讯录与组织架构、管理自己发布的日程、修改自己的密码 |

> 演示数据包含 **7 个部门**（含技术部下辖研发一组/二组）、**12 名员工**（11 在职 / 1 离职）、**5 个账号**、**5 条日程**（2 条公司公告 + 3 条部门/个人日程）。

---

## ✨ 功能清单

### 🔐 登录与权限

- 账号密码登录，密码使用 **BCrypt** 加盐哈希存储，数据库不保存明文
- 登录成功签发 **JWT**（HS256，自实现无第三方依赖），有效期 7 天
- 拦截器统一校验：未登录返回 `401`，前端路由守卫自动跳转登录页
- **权限变更立即生效**：每次请求回查用户状态与角色，账号被禁用/降权/删除后旧 token 立刻失效
- 退出登录、修改自己的密码（校验原密码 + 新密码长度）

### 🏢 部门管理

- 树形组织架构展示（支持多级，如 总经办 → 技术部 → 研发一组）
- 新增 / 修改 / 删除部门，**仅管理员**可操作
- 统计每个部门的员工人数
- **防成环校验**：上级部门不能设为自己或自己的任意深度子孙
- **删除保护**：部门下有员工或有子部门时拒绝删除，并给出明确中文提示

### 📇 员工通讯录

- 分页列表，支持按**姓名模糊**、**部门**、**在职状态**筛选
- 新增 / 修改 / 删除员工（仅管理员），记录自动带出所属部门名称
- 参数校验：姓名必填且 ≤20 字、手机号必须 11 位数字、邮箱必须含 `@`、入职日期格式校验
- **删除保护**：员工已关联系统账号时拒绝删除

### 📅 日程与公告

- **公司公告**：时间线形式展示，按时间倒序
- **日程管理**：分页列表，支持按标题、类型筛选
- 新增 / 修改 / 删除日程；发布人由后端从 token 获取（**前端伪造无效**）
- 权限规则：管理员可管理所有日程；普通员工**只能改/删自己发布的**；公司公告**仅管理员可发布**

### 👥 用户账号管理（仅管理员）

- 分页列表（**响应中永不包含密码字段**）
- 新增账号（账号唯一性校验、密码 BCrypt 加密）、重置密码、启用 / 禁用、删除
- 保护规则：不能禁用或删除自己的账号；发布过日程的账号不可删除

### 🙋 个人中心

- 展示我的信息（账号、姓名、部门、职位、手机号、邮箱、入职日期、角色）
- 修改密码（两次输入一致性校验）

---

## 🛠 技术栈

| 层次 | 技术 | 版本 | 说明 |
| :--- | :--- | :--- | :--- |
| 后端框架 | Spring Boot | 4.0.8 | 主流企业级开发框架 |
| 后端语言 | Java | 17 | |
| 数据访问 | MyBatis-Plus | 3.5.17 | 简化 CRUD，分页插件 |
| 数据库 | MySQL | 8.0 | utf8mb4 |
| 密码加密 | spring-security-crypto | BCrypt | 只取 crypto 模块，不引入完整 Security |
| 前端框架 | Vue | 3.5 | Composition API + `<script setup>` |
| 前端语言 | TypeScript | 5.7 | 严格模式 |
| 构建工具 | Vite | 7.3 | 开发服务器 / 生产构建 |
| UI 组件库 | Element Plus | 2.14 | 中文语言包 |
| 状态管理 | Pinia | 3.0 | 登录态管理 |
| 路由 | Vue Router | 4.6 | 含登录/角色守卫 |
| HTTP 客户端 | Axios | 1.20 | 统一封装拦截器 |

---

## 📂 项目结构

```
orgflow/
├── 后端框架/orgflow-backend/              # Spring Boot 后端
│   ├── sql/
│   │   ├── schema.sql                     # 建表脚本（4 张表）
│   │   └── init_data.sql                  # 演示数据（可重复执行）
│   ├── src/main/java/com/orgflow/
│   │   ├── common/                        # Result 统一返回、业务异常、全局异常处理
│   │   ├── config/                        # 跨域+拦截器、JWT、分页插件、Jackson 日期格式
│   │   ├── controller/                    # Auth / Department / Employee / Schedule / User
│   │   ├── dto/                           # 请求参数对象
│   │   ├── entity/                        # 4 个实体，对应 4 张表
│   │   ├── interceptor/                   # JwtInterceptor 登录拦截
│   │   ├── mapper/                        # MyBatis-Plus Mapper
│   │   ├── service/                       # 业务接口
│   │   │   └── impl/                      # 业务实现
│   │   ├── util/                          # JwtUtil / PasswordUtil / CurrentUserUtil
│   │   └── vo/                            # 响应对象（不含密码）
│   └── pom.xml
│
├── 前端框架/orgflow-web/                  # Vue 3 前端
│   ├── src/
│   │   ├── api/                           # 接口封装，与后端一一对应
│   │   ├── layout/Layout.vue              # 左侧菜单 + 顶部栏
│   │   ├── router/index.ts                # 路由与登录/角色守卫
│   │   ├── stores/auth.ts                 # Pinia 登录态
│   │   ├── utils/                         # request 封装、时间格式化
│   │   └── views/                         # 7 个页面
│   ├── vite.config.ts
│   └── package.json
│
├── 启动说明.md                            # 详细启动文档（含排错四步定位法）
├── 开发契约.md                            # 前后端接口契约（27 个接口）
├── START-HERE.bat                         # 一键启动（Windows）
└── orgflow-start-*.bat                    # 分别启动前后端
```

---

## 🚀 本地运行

### 环境要求

| 工具 | 版本 |
| :--- | :--- |
| JDK | 17（Spring Boot 4 要求 17+，**不要用更高版本**） |
| Maven | 3.6+ |
| MySQL | 8.0+ |
| Node.js | 18+ |
| pnpm | 8+ |

### 第 1 步：准备数据库

```sql
-- 建库
CREATE DATABASE IF NOT EXISTS orgflow
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后依次执行 `后端框架/orgflow-backend/sql/schema.sql`（建表）和 `init_data.sql`（演示数据）。

### 第 2 步：配置环境变量

数据库密码与 JWT 密钥都通过**环境变量**注入，代码中只有本机开发用的默认值：

```bash
# Linux / macOS
export MYSQL_HOST=localhost
export MYSQL_PORT=3306
export MYSQL_DATABASE=orgflow
export MYSQL_USERNAME=root
export MYSQL_PASSWORD=你的数据库密码
export ORGFLOW_JWT_SECRET=一串足够长的随机字符串   # 生产环境必须修改
```

```powershell
# Windows PowerShell
$env:MYSQL_PASSWORD="你的数据库密码"
$env:ORGFLOW_JWT_SECRET="一串足够长的随机字符串"
```

> ⚠️ `ORGFLOW_JWT_SECRET` 如果沿用默认值，任何人都能伪造管理员 token，**部署到公网前必须修改**。

### 第 3 步：启动后端

```bash
cd 后端框架/orgflow-backend
mvn -DskipTests clean package
java -jar target/orgflow-backend.jar
```

启动成功后访问 `http://localhost:8080`，看到接口返回即表示数据库连接正常。

### 第 4 步：启动前端

```bash
cd 前端框架/orgflow-web
pnpm install
pnpm run dev
```

浏览器打开 **http://localhost:5173**，用 `admin / 123456` 登录。

### Windows 一键启动

双击 `START-HERE.bat`：它会自动启动前后端并打开浏览器。

---

## 🔌 接口一览

完整契约见 [`开发契约.md`](开发契约.md)，共 27 个接口。统一返回格式：

```json
{ "code": 200, "message": "操作成功", "data": {} }
```

`code` 与 HTTP 状态码严格一致（200 成功 / 400 参数错误 / 401 未登录 / 403 无权限 / 404 不存在 / 500 服务器异常）。

| 模块 | 接口 |
| :--- | :--- |
| 认证 | `POST /api/auth/login`、`GET /api/auth/info`、`POST /api/auth/logout`、`PUT /api/auth/password` |
| 部门 | `GET /api/department/list`、`/tree`、`/{id}`、`POST`、`PUT /{id}`、`DELETE /{id}` |
| 员工 | `GET /api/employee/page`、`/options`、`/{id}`、`POST`、`PUT /{id}`、`DELETE /{id}` |
| 日程 | `GET /api/schedule/page`、`/announcements`、`/{id}`、`POST`、`PUT /{id}`、`DELETE /{id}` |
| 用户 | `GET /api/user/page`、`POST`、`PUT /{id}`、`PUT /{id}/status`、`DELETE /{id}` |

---

## 🔒 安全设计

| 措施 | 说明 |
| :--- | :--- |
| 密码存储 | BCrypt 加盐哈希，数据库不存明文 |
| 敏感字段隔离 | 所有对外 VO 不含 `password`（响应体经断言校验不出现该字段） |
| JWT 签名 | HS256 + 常数时间比较（`MessageDigest.isEqual`），防时序攻击；签名不依赖 header 的 alg，无算法混淆风险 |
| 权限实时性 | 拦截器每次请求回查数据库，禁用/降权/删除立即生效 |
| 三层鉴权 | ① 未登录 401 ② 角色校验（非管理员写操作 403）③ 数据归属校验（只能改自己的日程） |
| 引用完整性 | 删除部门/员工/用户前校验关联，防止出现悬空引用 |
| 自我保护 | 不能禁用自己、删除自己、降低自己的角色，避免系统失去管理员 |
| 异常处理 | 内部错误只返回固定文案，堆栈仅进服务端日志；畸形 JSON 不记录请求体 |

---

## 🧪 测试情况

开发过程中使用真实 HTTP 回归脚本进行验证：

| 项目 | 结果 |
| :--- | :--- |
| 后端接口断言 | **165 条全部通过** |
| 前后端联调断言 | **29 条全部通过** |
| 前端类型检查 | `vue-tsc` 零错误 |
| 前端生产构建 | `vite build` 成功 |

覆盖范围：正常流程、越权访问（伪造管理员公告 / 越权删除 / 自我降权）、参数校验、边界值、引用完整性保护、权限变更实时性。

---

## 📋 已知限制

- `PUT` 系列接口为**全量替换**语义：请求体缺失的字段会被视为清空。前端表单始终提交完整字段，正常使用不会丢数据。
- 个人中心只支持修改密码；修改手机号/邮箱需管理员在通讯录中操作。
- 未接入文件上传，员工无头像。

---

## 📄 开源协议

本项目为毕业设计作品，采用 MIT 协议，可自由学习参考。

<div align="center">

**如果这个项目对你有帮助，欢迎点个 ⭐ Star**

</div>
