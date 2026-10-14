/**
 * 路由配置（契约第 5 节）
 * - /login 登录页（无需 token）
 * - / 使用 Layout 布局，默认重定向到 /dashboard
 * - /department /employee /schedule /profile 为 Layout 子路由
 * - /user 用户管理，仅管理员（meta.roles: [1]）
 */
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ROLE_ADMIN } from '@/types'

/** 需要登录的路由白名单之外的都跳登录页 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'department',
        name: 'Department',
        component: () => import('@/views/Department.vue'),
        meta: { title: '部门管理' },
      },
      {
        path: 'employee',
        name: 'Employee',
        component: () => import('@/views/Employee.vue'),
        meta: { title: '通讯录' },
      },
      {
        path: 'schedule',
        name: 'Schedule',
        component: () => import('@/views/Schedule.vue'),
        meta: { title: '日程管理' },
      },
      {
        path: 'user',
        name: 'User',
        component: () => import('@/views/User.vue'),
        meta: { title: '用户管理', roles: [ROLE_ADMIN] },
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心' },
      },
    ],
  },
  // 兜底：未匹配到的路径回首页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  const isPublic = to.meta.public === true

  // 已登录访问 /login → 回首页
  if (to.path === '/login') {
    return auth.token ? { path: '/dashboard' } : true
  }

  // 未登录一律跳登录页
  if (!auth.token && !isPublic) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 有 token 但本机没有用户缓存（例如手动清了 localStorage）时先补一次
  if (auth.token && !auth.user) {
    try {
      await auth.fetchInfo()
    } catch {
      // fetchInfo 失败时 request.ts 已经处理了 401 跳转，这里只需放行到下一轮
      return true
    }
  }

  // meta.roles 限制：非授权角色跳首页
  const roles = to.meta.roles as number[] | undefined
  if (roles && roles.length > 0) {
    const role: number | undefined = auth.user?.role
    if (role === undefined || !roles.includes(role)) {
      return { path: '/dashboard' }
    }
  }

  return true
})

// 统一设置页面标题
router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? ''
  document.title = title
    ? `${title} - 企程 OrgFlow`
    : '企程 OrgFlow - 公司部门通讯录与日程管理系统'
})

export default router
