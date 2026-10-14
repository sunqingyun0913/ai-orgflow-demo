/**
 * 用户管理接口（契约第 4.5 节）
 * 整个 /api/user 仅管理员可访问。
 */
import { del, get, post, put } from '@/utils/request'
import type { PageResult, UserRecord } from '@/types'

/** 用户分页查询参数 */
export interface UserPageParams {
  current?: number
  size?: number
  username?: string
}

/** 新增用户的数据 */
export interface UserCreateForm {
  username: string
  password: string
  employeeId: number | null
  role: number
  status: number
}

/** 修改用户的数据（password 为空表示不修改密码） */
export interface UserUpdateForm {
  password?: string
  employeeId: number | null
  role: number
  status: number
}

/** 用户分页查询 */
export function pageUsers(params: UserPageParams): Promise<PageResult<UserRecord>> {
  return get<PageResult<UserRecord>>('/user/page', params as Record<string, unknown>)
}

/** 新增用户 */
export function createUser(data: UserCreateForm): Promise<UserRecord> {
  return post<UserRecord>('/user', data)
}

/** 修改用户（password 为空则不修改密码） */
export function updateUser(id: number, data: UserUpdateForm): Promise<UserRecord> {
  return put<UserRecord>(`/user/${id}`, data)
}

/** 启用/禁用用户 */
export function updateUserStatus(id: number, status: number): Promise<null> {
  return put<null>(`/user/${id}/status`, { status })
}

/** 删除用户（不能删除自己） */
export function deleteUser(id: number): Promise<null> {
  return del<null>(`/user/${id}`)
}
