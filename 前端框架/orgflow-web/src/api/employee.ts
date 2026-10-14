/**
 * 员工接口（契约第 4.3 节）
 * 注意：新增/修改/删除仅管理员可用。
 */
import { del, get, post, put } from '@/utils/request'
import type { Employee, EmployeeOption, PageResult } from '@/types'

/** 员工分页查询参数 */
export interface EmployeePageParams {
  current?: number
  size?: number
  name?: string
  departmentId?: number | null
  status?: number | null
  /** 传 1 时后端把 size 强制为 500，返回最多 500 条在职员工 */
  assignee?: number
}

/** 员工新增/修改的表单数据 */
export interface EmployeeForm {
  name: string
  departmentId: number | null
  phone: string
  email: string
  position: string
  entryDate: string
  status: number
}

/** 员工分页查询 */
export function pageEmployees(params: EmployeePageParams): Promise<PageResult<Employee>> {
  return get<PageResult<Employee>>('/employee/page', params as Record<string, unknown>)
}

/** 员工下拉选项（在职员工，id + name + departmentName） */
export function listEmployeeOptions(): Promise<EmployeeOption[]> {
  return get<EmployeeOption[]>('/employee/options')
}

/** 员工详情 */
export function getEmployee(id: number): Promise<Employee> {
  return get<Employee>(`/employee/${id}`)
}

/** 新增员工（仅管理员） */
export function createEmployee(data: EmployeeForm): Promise<Employee> {
  return post<Employee>('/employee', data)
}

/** 修改员工（仅管理员） */
export function updateEmployee(id: number, data: EmployeeForm): Promise<Employee> {
  return put<Employee>(`/employee/${id}`, data)
}

/** 删除员工（仅管理员） */
export function deleteEmployee(id: number): Promise<null> {
  return del<null>(`/employee/${id}`)
}
