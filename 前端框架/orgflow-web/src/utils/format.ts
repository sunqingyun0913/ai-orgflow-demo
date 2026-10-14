/**
 * 日期时间格式化工具
 * 后端时间字段可能是 `2026-10-10T07:55:27`（ISO）或 `2026-10-10 07:55:27`，
 * 两种都要能正确解析，统一输出 `yyyy-MM-dd HH:mm:ss` / `yyyy-MM-dd`。
 */

function pad(n: number): string {
  return n < 10 ? `0${n}` : String(n)
}

/**
 * 把后端返回的时间值解析成 Date。
 * 兼容：
 * - `2026-10-10 07:55:27`（空格分隔，Safari 不认，手动换成 T）
 * - `2026-10-10T07:55:27` / `2026-10-10T07:55:27.000Z`（ISO）
 * - `2026-10-10`（纯日期）
 * - 时间戳数字
 * 解析不出来返回 null。
 */
export function parseTime(value?: string | number | Date | null): Date | null {
  if (value === null || value === undefined || value === '') return null
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : value

  if (typeof value === 'number') {
    const d = new Date(value)
    return Number.isNaN(d.getTime()) ? null : d
  }

  const text = String(value).trim()
  if (text === '') return null

  // 已经是 2026-10-10 07:55:27 这种格式：补上 T 让各浏览器都能解析
  const normalized = /^\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}(:\d{2})?$/.test(text)
    ? text.replace(' ', 'T')
    : text

  const d = new Date(normalized)
  return Number.isNaN(d.getTime()) ? null : d
}

/** 格式化为 `yyyy-MM-dd HH:mm:ss`，解析失败返回空串 */
export function formatDateTime(value?: string | number | Date | null): string {
  const d = parseTime(value)
  if (!d) return ''
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ` +
    `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
  )
}

/** 格式化为 `yyyy-MM-dd`，解析失败返回空串 */
export function formatDate(value?: string | number | Date | null): string {
  const d = parseTime(value)
  if (!d) return ''
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

/** 截断长文本，用于公告内容摘要 */
export function ellipsis(text?: string | null, max = 80): string {
  const s = (text ?? '').trim()
  if (s.length <= max) return s
  return `${s.slice(0, max)}…`
}
