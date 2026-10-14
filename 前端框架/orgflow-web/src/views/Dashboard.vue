<script setup lang="ts">
/**
 * 首页
 * - 欢迎语：你好，<姓名>（<角色>）
 * - 4 个统计卡片：部门数 / 在职员工数 / 公司公告数 / 日程总数
 * - 最新公告列表（前 3 条）
 * 数据来源全部为契约第 4 节的真实接口，无假数据。
 */
import { computed, onMounted, ref } from 'vue'
import { Calendar, OfficeBuilding, User, UserFilled } from '@element-plus/icons-vue'
import type { Component } from 'vue'
import { listDepartments } from '@/api/department'
import { pageEmployees } from '@/api/employee'
import { listAnnouncements, pageSchedules } from '@/api/schedule'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, ellipsis } from '@/utils/format'
import { EMPLOYEE_ACTIVE, type Schedule } from '@/types'

const auth = useAuthStore()

/** 统计卡片数据（数据结构与后台数据都来自接口） */
const loading = ref(false)
const announcementLoading = ref(false)

const departmentCount = ref(0)
const activeEmployeeCount = ref(0)
const announcementCount = ref(0)
const scheduleTotal = ref(0)

/** 最新公告（最多 3 条） */
const announcements = ref<Schedule[]>([])

/** 欢迎语姓名：优先员工姓名，其次账号 */
const welcomeName = computed(() => auth.employee?.name || auth.user?.username || '用户')
/** 部门 + 职位，没有员工信息时不显示 */
const welcomePost = computed(() => {
  const dept = auth.employee?.departmentName
  const position = auth.employee?.position
  const parts = [dept, position].filter((p): p is string => !!p)
  return parts.join(' · ')
})

/** 当前日期，例如 2026年10月10日 星期五 */
const today = computed(() => {
  const d = new Date()
  const week = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 ${week[d.getDay()]}`
})

interface StatItem {
  key: string
  label: string
  value: number
  icon: Component
  color: string
}

const stats = computed<StatItem[]>(() => [
  {
    key: 'department',
    label: '部门数量',
    value: departmentCount.value,
    icon: OfficeBuilding,
    color: '#2f6fed',
  },
  {
    key: 'employee',
    label: '在职员工',
    value: activeEmployeeCount.value,
    icon: User,
    color: '#0ea5a4',
  },
  {
    key: 'announcement',
    label: '公司公告',
    value: announcementCount.value,
    icon: UserFilled,
    color: '#f59e0b',
  },
  {
    key: 'schedule',
    label: '日程总数',
    value: scheduleTotal.value,
    icon: Calendar,
    color: '#8b5cf6',
  },
])

/** 最新 3 条公告 */
const latestAnnouncements = computed(() => announcements.value.slice(0, 3))

/** 公告摘要 */
function summary(content?: string | null): string {
  return ellipsis(content, 60) || '（无内容）'
}

/**
 * 加载首页统计
 * - 部门数：/api/department/list 的 length
 * - 在职员工数：/api/employee/page?assignee=1，优先用 total，缺失时按 status===1 过滤
 * - 公告数：/api/schedule/announcements 的 length
 * - 日程总数：/api/schedule/page?current=1&size=1 的 total
 */
async function loadStats(): Promise<void> {
  loading.value = true
  const tasks = [
    listDepartments()
      .then((list) => {
        departmentCount.value = list?.length ?? 0
      })
      .catch(() => undefined),
    pageEmployees({ current: 1, size: 10, assignee: 1 })
      .then((page) => {
        const records = page?.records ?? []
        // assignee=1 时后端返回最多 500 条在职员工，total 与 records.length 都可用
        activeEmployeeCount.value =
          typeof page?.total === 'number' ? page.total : records.filter((r) => r.status === EMPLOYEE_ACTIVE).length
      })
      .catch(() => undefined),
    listAnnouncements()
      .then((list) => {
        announcements.value = list ?? []
        announcementCount.value = list?.length ?? 0
      })
      .catch(() => undefined),
    pageSchedules({ current: 1, size: 1 })
      .then((page) => {
        scheduleTotal.value = page?.total ?? 0
      })
      .catch(() => undefined),
  ]
  await Promise.all(tasks)
  loading.value = false
}

/** 只加载公告区（单独控制骨架屏） */
async function loadAnnouncements(): Promise<void> {
  announcementLoading.value = true
  try {
    const list = await listAnnouncements()
    announcements.value = list ?? []
    announcementCount.value = list?.length ?? 0
  } catch {
    // request.ts 已提示
  } finally {
    announcementLoading.value = false
  }
}

onMounted(async () => {
  // 刷新页面后如果 store 里没有员工信息，补拉一次，保证欢迎语与个人中心数据完整
  if (auth.token && !auth.employee) {
    try {
      await auth.fetchInfo()
    } catch {
      // 忽略：401 时 request.ts 会跳登录页
    }
  }
  await Promise.all([loadStats(), loadAnnouncements()])
})
</script>

<template>
  <div class="dashboard">
    <!-- 欢迎条 -->
    <div class="dashboard__hero of-card">
      <div class="dashboard__hero-left">
        <h2 class="dashboard__hello">
          你好，{{ welcomeName }}
          <span class="dashboard__role">（{{ auth.roleName }}）</span>
        </h2>
        <p class="dashboard__welcome">
          欢迎使用企程 OrgFlow 公司部门通讯录与日程管理系统
          <template v-if="welcomePost">，你所在的岗位：{{ welcomePost }}</template>
        </p>
      </div>
      <div class="dashboard__date">{{ today }}</div>
    </div>

    <!-- 统计卡片 -->
    <div v-loading="loading" class="dashboard__stats">
      <div v-for="item in stats" :key="item.key" class="dashboard__stat of-card">
        <div class="dashboard__stat-icon" :style="{ '--stat-color': item.color }">
          <el-icon :size="24"><component :is="item.icon" /></el-icon>
        </div>
        <div class="dashboard__stat-body">
          <div class="dashboard__stat-value">{{ item.value }}</div>
          <div class="dashboard__stat-label">{{ item.label }}</div>
        </div>
      </div>
    </div>

    <!-- 最新公告 -->
    <div class="of-card dashboard__announcements">
      <div class="of-card__header">
        <div class="of-card__title">
          <el-icon><Bell /></el-icon>
          最新公告
        </div>
        <el-button type="primary" link @click="loadAnnouncements">刷新</el-button>
      </div>

      <el-skeleton v-if="announcementLoading" :rows="3" animated />

      <template v-else>
        <el-empty
          v-if="latestAnnouncements.length === 0"
          description="暂无公司公告"
          :image-size="80"
        />
        <ul v-else class="dashboard__announcement-list">
          <li
            v-for="item in latestAnnouncements"
            :key="item.id"
            class="dashboard__announcement"
          >
            <div class="dashboard__announcement-main">
              <el-tag size="small" type="warning" effect="light">公告</el-tag>
              <span class="dashboard__announcement-title">{{ item.title }}</span>
            </div>
            <span class="dashboard__announcement-time">
              {{ formatDateTime(item.startTime ?? item.createTime) }}
            </span>
            <p class="dashboard__announcement-desc">{{ summary(item.content) }}</p>
          </li>
        </ul>
      </template>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.dashboard__hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: linear-gradient(120deg, #ffffff 0%, #f2f7ff 100%);
}

.dashboard__hello {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--of-text);
}

.dashboard__role {
  font-size: 14px;
  font-weight: 500;
  color: var(--of-text-secondary);
}

.dashboard__welcome {
  margin: 0;
  font-size: 13px;
  color: var(--of-text-secondary);
}

.dashboard__date {
  font-size: 13px;
  color: var(--of-text-secondary);
  white-space: nowrap;
}

.dashboard__stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  min-height: 96px;
}

.dashboard__stat {
  display: flex;
  align-items: center;
  gap: 14px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.dashboard__stat:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.1);
}

.dashboard__stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--stat-color);
  background: color-mix(in srgb, var(--stat-color) 12%, #ffffff);
}

.dashboard__stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--of-text);
  line-height: 1.2;
}

.dashboard__stat-label {
  font-size: 13px;
  color: var(--of-text-secondary);
  margin-top: 2px;
}

.dashboard__announcement-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.dashboard__announcement {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-areas:
    'main time'
    'desc desc';
  gap: 4px 12px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--of-border);
}

.dashboard__announcement:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.dashboard__announcement-main {
  grid-area: main;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.dashboard__announcement-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--of-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard__announcement-time {
  grid-area: time;
  font-size: 12px;
  color: var(--of-text-secondary);
  white-space: nowrap;
}

.dashboard__announcement-desc {
  grid-area: desc;
  margin: 0;
  font-size: 13px;
  color: var(--of-text-secondary);
  line-height: 1.5;
}

@media (max-width: 1100px) {
  .dashboard__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
