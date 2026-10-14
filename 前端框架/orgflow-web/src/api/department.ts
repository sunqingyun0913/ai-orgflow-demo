/**
 * 部门接口（契约第 4.2 节）
 * 注意：新增/修改/删除仅管理员可用，普通员工调用会返回 403。
 */
import { del, get, post, put } from '@/utils/request'
import type { Department, DepartmentTreeNode } from '@/types'

/** 部门全量列表（按 sort_order,id 排序，含 employeeCount） */
export function listDepartments(): Promise<Department[]> {
  return get<Department[]>('/department/list')
}

/** 部门树 */
export function getDepartmentTree(): Promise<DepartmentTreeNode[]> {
  return get<DepartmentTreeNode[]>('/department/tree')
}

/** 部门详情 */
export function getDepartment(id: number): Promise<Department> {
  return get<Department>(`/department/${id}`)
}

/** 新增部门（仅管理员）；parentId 传 0 表示顶级部门 */
export function createDepartment(data: {
  name: string
  parentId: number
  sortOrder: number
}): Promise<Department> {
  return post<Department>('/department', data)
}

/** 修改部门（仅管理员） */
export function updateDepartment(
  id: number,
  data: { name: string; parentId: number; sortOrder: number },
): Promise<Department> {
  return put<Department>(`/department/${id}`, data)
}

/** 删除部门（仅管理员）；该部门下还有员工时后端返回 400 */
export function deleteDepartment(id: number): Promise<null> {
  return del<null>(`/department/${id}`)
}
