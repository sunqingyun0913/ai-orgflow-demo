/**
 * 认证接口（契约第 4.1 节）
 */
import { get, post, put } from '@/utils/request'
import type { LoginResult, UserInfo } from '@/types'

/** 登录（无需 token 放行接口） */
export function login(data: { username: string; password: string }): Promise<LoginResult> {
  return post<LoginResult>('/auth/login', data)
}

/** 获取当前登录用户信息 */
export function getUserInfo(): Promise<UserInfo> {
  return get<UserInfo>('/auth/info')
}

/** 退出登录（后端无状态 JWT，调用后由前端清本地登录态） */
export function logout(): Promise<null> {
  return post<null>('/auth/logout')
}

/** 修改自己的密码（newPassword 至少 6 位） */
export function updatePassword(data: {
  oldPassword: string
  newPassword: string
}): Promise<null> {
  return put<null>('/auth/password', data)
}
