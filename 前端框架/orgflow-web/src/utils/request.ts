/**
 * Axios 封装
 * - baseURL /api
 * - 请求拦截器：从 localStorage 取 token，加 Authorization: Bearer <token>
 * - 响应拦截器：code === 200 时直接 resolve 后端的 data；否则 ElMessage.error(message) 并 reject
 * - HTTP 401 或 code === 401：清空登录态并跳 /login
 */
import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types'

/** localStorage 中 token 的键名（stores/auth.ts 与 request.ts 共用） */
export const TOKEN_KEY = 'orgflow_token'
/** localStorage 中用户信息的键名 */
export const USER_KEY = 'orgflow_user'
/** localStorage 中员工信息的键名 */
export const EMPLOYEE_KEY = 'orgflow_employee'

/** 401 时清空本地登录态并跳登录页 */
function toLogin(): void {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(EMPLOYEE_KEY)
  // 用 location 跳转而不是 router，避免 request -> router -> store -> request 的循环依赖
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器：附加 token
instance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error: unknown) => Promise.reject(error),
)

// 响应拦截器：拆统一返回格式
instance.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult<unknown> | undefined
    // 后端约定所有接口都返回 { code, message, data }
    if (res && typeof res.code === 'number') {
      if (res.code === 200) {
        // 成功：只把 data 交给业务层
        return res.data as never
      }
      if (res.code === 401) {
        ElMessage.error(res.message || '登录已失效，请重新登录')
        toLogin()
        return Promise.reject(new Error(res.message || '登录已失效'))
      }
      ElMessage.error(res.message || '操作失败')
      return Promise.reject(new Error(res.message || '操作失败'))
    }
    // 极少数非标准返回（例如静态资源）直接透传
    return response.data as never
  },
  (error: unknown) => {
    const err = error as {
      response?: { status?: number; data?: ApiResult<unknown> }
      message?: string
    }
    const status = err.response?.status
    const backendMessage = err.response?.data?.message

    if (status === 401) {
      ElMessage.error(backendMessage || '登录已失效，请重新登录')
      toLogin()
      return Promise.reject(error)
    }
    if (status === 403) {
      ElMessage.error(backendMessage || '没有操作权限')
      return Promise.reject(error)
    }
    if (status === 400) {
      ElMessage.error(backendMessage || '请求参数有误')
      return Promise.reject(error)
    }
    ElMessage.error(backendMessage || err.message || '网络异常，请稍后重试')
    return Promise.reject(error)
  },
)

/**
 * 统一请求方法：返回值已经是后端的 data 字段。
 * 泛型 T 表示后端 data 的类型。
 */
export function request<T>(config: AxiosRequestConfig): Promise<T> {
  return instance.request(config) as unknown as Promise<T>
}

/** GET 请求 */
export function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  return request<T>({ url, method: 'get', params })
}

/** POST 请求 */
export function post<T>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'post', data })
}

/** PUT 请求 */
export function put<T>(url: string, data?: unknown): Promise<T> {
  return request<T>({ url, method: 'put', data })
}

/** DELETE 请求 */
export function del<T>(url: string): Promise<T> {
  return request<T>({ url, method: 'delete' })
}

export default instance
