/**
 * 登录态 store
 * token / user / employee 都持久化到 localStorage，刷新页面后自动恢复；
 * request.ts 的请求拦截器直接从 localStorage 读 token，所以两者用同一套键名。
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { EMPLOYEE_KEY, TOKEN_KEY, USER_KEY } from '@/utils/request'
import { ROLE_ADMIN, type EmployeeBrief, type LoginResult, type UserInfo } from '@/types'

/** 安全解析 localStorage 中的 JSON */
function readJson<T>(key: string): T | null {
  const raw = localStorage.getItem(key)
  if (!raw) return null
  try {
    return JSON.parse(raw) as T
  } catch {
    // 数据被污染时直接丢弃，避免整个应用崩掉
    localStorage.removeItem(key)
    return null
  }
}

export const useAuthStore = defineStore('auth', () => {
  /** JWT token */
  const token = ref<string>(localStorage.getItem(TOKEN_KEY) ?? '')
  /** 当前登录用户（user 表） */
  const user = ref<UserInfo | null>(readJson<UserInfo>(USER_KEY))
  /** 当前用户关联的员工信息（可能为 null） */
  const employee = ref<EmployeeBrief | null>(readJson<EmployeeBrief>(EMPLOYEE_KEY))

  /** 是否已登录 */
  const isLoggedIn = computed(() => !!token.value)
  /** 是否管理员（role === 1） */
  const isAdmin = computed(() => user.value?.role === ROLE_ADMIN)
  /** 显示名称：优先员工姓名，其次账号 */
  const displayName = computed(() => employee.value?.name || user.value?.username || '用户')
  /** 角色中文名 */
  const roleName = computed(() => (isAdmin.value ? '管理员' : '普通员工'))

  /** 把登录态写入 state 与 localStorage */
  function setAuth(payload: {
    token?: string
    user?: UserInfo | null
    employee?: EmployeeBrief | null
  }): void {
    if (payload.token !== undefined) {
      token.value = payload.token
      localStorage.setItem(TOKEN_KEY, payload.token)
    }
    if (payload.user !== undefined) {
      user.value = payload.user
      if (payload.user) {
        localStorage.setItem(USER_KEY, JSON.stringify(payload.user))
      } else {
        localStorage.removeItem(USER_KEY)
      }
    }
    if (payload.employee !== undefined) {
      employee.value = payload.employee
      if (payload.employee) {
        localStorage.setItem(EMPLOYEE_KEY, JSON.stringify(payload.employee))
      } else {
        localStorage.removeItem(EMPLOYEE_KEY)
      }
    }
  }

  /** 清空登录态 */
  function clearAuth(): void {
    token.value = ''
    user.value = null
    employee.value = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    localStorage.removeItem(EMPLOYEE_KEY)
  }

  /** 登录：写入 token + user + employee */
  async function login(username: string, password: string): Promise<LoginResult> {
    const data = await authApi.login({ username, password })
    // 契约形状是 { token, user, employee }；后端 LoginVO 实际是 { token, user(内含 employee), displayName }
    // 两种都兼容：优先取平铺的 employee，取不到再取 user.employee
    const rawEmployee = data.employee ?? data.user?.employee ?? null
    const brief: EmployeeBrief | null = rawEmployee
      ? {
          id: rawEmployee.id,
          name: rawEmployee.name ?? data.displayName ?? data.user?.username ?? '',
          position: rawEmployee.position ?? null,
          departmentId: rawEmployee.departmentId ?? null,
          departmentName: rawEmployee.departmentName ?? null,
        }
      : null
    setAuth({ token: data.token, user: data.user, employee: brief })
    return data
  }

  /**
   * 拉取当前登录用户信息，用于刷新页面后补全 user/employee。
   * 契约：data = { id,username,employeeId,role,status,employee:{...}|null }
   */
  async function fetchInfo(): Promise<UserInfo> {
    const data = await authApi.getUserInfo()
    // 把嵌套的 employee 拍平，方便页面直接用 employee.name / departmentName
    const brief: EmployeeBrief | null = data.employee
      ? {
          id: data.employee.id,
          name: data.employee.name,
          position: data.employee.position ?? null,
          departmentId: data.employee.departmentId ?? null,
          departmentName: data.employee.departmentName ?? null,
        }
      : null
    setAuth({ user: { ...data, employee: undefined }, employee: brief })
    return data
  }

  /** 退出登录：真调后端接口，无论成功失败都清本地登录态 */
  async function logout(): Promise<void> {
    try {
      await authApi.logout()
    } catch {
      // 后端不可用或 token 已失效时也要能退出，忽略异常
    } finally {
      clearAuth()
    }
  }

  return {
    token,
    user,
    employee,
    isLoggedIn,
    isAdmin,
    displayName,
    roleName,
    setAuth,
    clearAuth,
    login,
    fetchInfo,
    logout,
  }
})
