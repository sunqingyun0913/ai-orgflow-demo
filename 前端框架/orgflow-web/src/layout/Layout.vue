<script setup lang="ts">
/**
 * 整体布局：左侧可折叠菜单 + 顶部面包屑 + 右上角用户下拉
 */
import { computed, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { icons } from '@/icons'
import { ROLE_ADMIN } from '@/types'

interface MenuItem {
  path: string
  title: string
  /** 图标组件名，取自 @element-plus/icons-vue */
  icon: string
  /** 仅这些角色可见；不填表示所有登录用户可见 */
  roles?: number[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

/** 侧边栏折叠状态 */
const collapsed = ref(false)

/** 左侧菜单：用户管理仅管理员可见 */
const menus: MenuItem[] = [
  { path: '/dashboard', title: '首页', icon: 'HomeFilled' },
  { path: '/department', title: '部门管理', icon: 'OfficeBuilding' },
  { path: '/employee', title: '通讯录', icon: 'User' },
  { path: '/schedule', title: '日程管理', icon: 'Calendar' },
  { path: '/user', title: '用户管理', icon: 'Setting', roles: [ROLE_ADMIN] },
  { path: '/profile', title: '个人中心', icon: 'UserFilled' },
]

/** 按角色过滤后的菜单 */
const visibleMenus = computed(() =>
  menus.filter((m) => !m.roles || m.roles.includes(auth.user?.role ?? 0)),
)

/** 当前高亮菜单项 */
const activeMenu = computed(() => route.path)

/** 面包屑：首页固定，当前路由追加一项 */
const breadcrumbs = computed(() => {
  const title = (route.meta.title as string | undefined) ?? ''
  if (!title || title === '首页') return [{ title: '首页' }]
  return [{ title: '首页' }, { title }]
})

/** 取图标组件 */
function iconOf(name: string): Component | undefined {
  return icons[name]
}

/** 顶部用户下拉命令 */
async function handleUserCommand(command: string | number | object): Promise<void> {
  if (command === 'profile') {
    router.push('/profile')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      // 用户点了取消
      return
    }
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<template>
  <el-container class="layout">
    <!-- 左侧菜单 -->
    <el-aside :width="collapsed ? '64px' : '210px'" class="layout__aside">
      <div class="layout__logo" :title="collapsed ? '企程 OrgFlow' : undefined">
        <el-icon class="layout__logo-icon"><OfficeBuilding /></el-icon>
        <span v-show="!collapsed" class="layout__logo-text">企程 OrgFlow</span>
      </div>

      <el-menu
        class="layout__menu"
        :default-active="activeMenu"
        :collapse="collapsed"
        :collapse-transition="false"
        router
      >
        <el-menu-item v-for="item in visibleMenus" :key="item.path" :index="item.path">
          <el-icon>
            <component :is="iconOf(item.icon)" />
          </el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="layout__body">
      <!-- 顶部栏 -->
      <el-header class="layout__header">
        <div class="layout__header-left">
          <el-icon class="layout__collapse" @click="collapsed = !collapsed">
            <Expand v-if="collapsed" />
            <Fold v-else />
          </el-icon>

          <el-breadcrumb separator="/">
            <el-breadcrumb-item v-for="(item, i) in breadcrumbs" :key="i">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="layout__header-right">
          <el-dropdown trigger="click" @command="handleUserCommand">
            <span class="layout__user">
              <el-avatar :size="30" class="layout__avatar">
                {{ auth.displayName.charAt(0) }}
              </el-avatar>
              <span class="layout__username">{{ auth.displayName }}</span>
              <el-tag size="small" :type="auth.isAdmin ? 'danger' : 'info'" effect="light">
                {{ auth.roleName }}
              </el-tag>
              <el-icon class="layout__arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 内容区 -->
      <el-main class="layout__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100vh;
}

.layout__aside {
  background: var(--of-sidebar-bg);
  border-right: 1px solid var(--of-border);
  overflow-x: hidden;
  transition: width 0.25s ease;
}

:deep(.layout__aside .el-menu) {
  border-right: none;
  height: 100%;
}

.layout__logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border-bottom: 1px solid var(--of-border);
  overflow: hidden;
}

.layout__logo-icon {
  font-size: 22px;
  color: var(--of-primary);
  flex-shrink: 0;
}

.layout__logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--of-primary);
  white-space: nowrap;
}

.layout__body {
  min-width: 0;
}

.layout__header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--of-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.layout__header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.layout__collapse {
  font-size: 18px;
  cursor: pointer;
  color: var(--of-text-secondary);
}

.layout__collapse:hover {
  color: var(--of-primary);
}

.layout__header-right {
  display: flex;
  align-items: center;
}

.layout__user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.layout__avatar {
  background: var(--of-primary);
  color: #fff;
  font-size: 14px;
}

.layout__username {
  font-size: 14px;
  color: var(--of-text);
}

.layout__arrow {
  font-size: 12px;
  color: var(--of-text-secondary);
}

.layout__main {
  background: var(--of-bg);
  padding: 16px;
  overflow-y: auto;
}
</style>
