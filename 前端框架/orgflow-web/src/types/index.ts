/**
 * 全局业务类型定义
 * 字段名严格对应《开发契约.md》第 4 节接口清单，不要随意改名。
 */

/** 后端统一返回格式：{ code, message, data }（契约第 2 节） */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

/** 分页数据结构（契约第 2 节） */
export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/** 角色：1 管理员 2 普通员工（契约第 1 节 user.role） */
export const ROLE_ADMIN = 1
export const ROLE_EMPLOYEE = 2

/** 账号状态：1 启用 0 禁用（契约第 1 节 user.status） */
export const ACCOUNT_ENABLED = 1
export const ACCOUNT_DISABLED = 0

/**
 * 员工状态：1 在职 0 离职
 *
 * 注意：契约第 4.3 节写的是「1 在职 / 2 离职」，但后端 EmployeeServiceImpl
 * （STATUS_ACTIVE=1、STATUS_INACTIVE=0，校验提示「只能是 1（在职）或 0（离职）」）
 * 与 sql/init_data.sql 的演示数据（如陈刚 status=0 表示离职）实际都用 0 表示离职。
 * 前端按后端真实行为取 0，否则「离职」筛选会查不到数据、列表标签也会显示错误。
 * （已把该契约不一致同步给 Lead）
 */
export const EMPLOYEE_ACTIVE = 1
export const EMPLOYEE_RESIGNED = 0

/** 日程类型：1 公司公告 2 个人/部门日程（契约第 1 节 schedule.type） */
export const SCHEDULE_ANNOUNCEMENT = 1
export const SCHEDULE_PERSONAL = 2

/** 部门（契约第 4.2 节，列表接口额外带 employeeCount） */
export interface Department {
  id: number
  name: string
  parentId: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
  employeeCount?: number
  /** 树接口返回的子节点 */
  children?: Department[]
}

/** 部门树的节点（契约第 4.2 节 /tree）
 * tree 与 list 按 id 合并后，节点上还会带上 employeeCount / createTime，故这里一并声明。
 */
export interface DepartmentTreeNode {
  id: number
  name: string
  parentId: number
  sortOrder?: number
  employeeCount?: number
  createTime?: string
  updateTime?: string
  /** 供 el-tree-select 使用：编辑部门时用来禁用自己及子孙节点 */
  disabled?: boolean
  children?: DepartmentTreeNode[]
}

/** 员工（契约第 4.3 节，每条记录额外带 departmentName） */
export interface Employee {
  id: number
  name: string
  departmentId?: number | null
  departmentName?: string | null
  phone?: string | null
  email?: string | null
  position?: string | null
  entryDate?: string | null
  status?: number
  createTime?: string
  updateTime?: string
}

/** 员工下拉选项（契约第 4.3 节 /options） */
export interface EmployeeOption {
  id: number
  name: string
  departmentName?: string | null
}

/** 日程（契约第 4.4 节，详情带 createByName） */
export interface Schedule {
  id: number
  title: string
  content?: string | null
  type: number
  startTime?: string | null
  endTime?: string | null
  createBy?: number | null
  createByName?: string | null
  createTime?: string
  updateTime?: string
  /** 当前登录用户是否可编辑/删除（无则为 false） */
  canEdit?: boolean
}

/** 员工简要信息（登录返回 / info 返回里嵌套的 employee） */
export interface EmployeeBrief {
  id: number
  name: string
  position?: string | null
  departmentId?: number | null
  departmentName?: string | null
}

/** 用户基本信息（契约第 4.1 节） */
export interface UserInfo {
  id: number
  username: string
  employeeId?: number | null
  role: number
  status?: number
  employee?: EmployeeBrief | null
}

/** 登录返回（契约第 4.1 节 /login）
 *
 * 契约写的是 { token, user, employee }，但后端 LoginVO 实际返回
 * { token, user: UserVO(内含 employee), displayName }（AuthController.login → Result<LoginVO>）。
 * 这里两个字段都声明成可选，登录时按「优先取平铺 employee，其次取 user.employee」兼容两种形状。
 */
export interface LoginResult {
  token: string
  user: UserInfo
  employee?: EmployeeBrief | null
  /** 后端返回的当前登录人姓名（LoginVO.displayName），用于打招呼兜底 */
  displayName?: string | null
}

/** 用户管理列表记录（契约第 4.5 节，额外带关联员工姓名） */
export interface UserRecord extends UserInfo {
  employeeName?: string | null
  createTime?: string
  updateTime?: string
}
