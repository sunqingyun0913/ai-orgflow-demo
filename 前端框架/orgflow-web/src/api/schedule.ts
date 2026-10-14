/**
 * 日程接口（契约第 4.4 节）
 * 权限：管理员可增删改任何日程；普通员工只能改/删自己发布的（create_by = 自己的 user id）。
 * 公司公告（type=1）仅管理员可发布。createBy 由后端从 token 取，前端不传。
 */
import { del, get, post, put } from '@/utils/request'
import type { PageResult, Schedule } from '@/types'

/** 日程分页查询参数 */
export interface SchedulePageParams {
  current?: number
  size?: number
  title?: string
  type?: number | null
}

/** 日程新增/修改的表单数据 */
export interface ScheduleForm {
  title: string
  content: string
  type: number
  startTime: string
  endTime: string
}

/** 日程分页查询 */
export function pageSchedules(params: SchedulePageParams): Promise<PageResult<Schedule>> {
  return get<PageResult<Schedule>>('/schedule/page', params as Record<string, unknown>)
}

/** 全部公司公告（type=1，按 start_time 倒序） */
export function listAnnouncements(): Promise<Schedule[]> {
  return get<Schedule[]>('/schedule/announcements')
}

/** 日程详情（带 createByName） */
export function getSchedule(id: number): Promise<Schedule> {
  return get<Schedule>(`/schedule/${id}`)
}

/** 新增日程 */
export function createSchedule(data: ScheduleForm): Promise<Schedule> {
  return post<Schedule>('/schedule', data)
}

/** 修改日程 */
export function updateSchedule(id: number, data: ScheduleForm): Promise<Schedule> {
  return put<Schedule>(`/schedule/${id}`, data)
}

/** 删除日程 */
export function deleteSchedule(id: number): Promise<null> {
  return del<null>(`/schedule/${id}`)
}
