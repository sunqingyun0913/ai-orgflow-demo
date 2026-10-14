<script setup lang="ts">
/**
 * 登录页
 * - 卡片式布局，标题「企程 OrgFlow」
 * - 回车直接提交
 * - 页面直接给出演示账号，方便答辩演示
 * - 密码错误时由 request.ts 统一弹出后端返回的 message
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

/** 演示账号（答辩用，直接显示在页面上） */
const demoAccounts = [
  { label: '管理员', username: 'admin', password: '123456' },
  { label: '普通员工', username: 'zhangsan', password: '123456' },
]

const rules: FormRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于 6 位', trigger: 'blur' },
  ],
}

/** 点击演示账号：一键填入 */
function fillDemo(item: { username: string; password: string }): void {
  form.username = item.username
  form.password = item.password
  formRef.value?.clearValidate()
}

/** 提交登录 */
async function handleLogin(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    // 登录前想去的页面（被守卫拦下时会带 redirect 参数）
    const redirect = route.query.redirect
    const target = typeof redirect === 'string' && redirect ? redirect : '/dashboard'
    await router.push(target)
  } catch {
    // request.ts 的响应拦截器已经弹出后端 message，这里不再重复提示
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <el-card class="login__card" shadow="always">
      <div class="login__header">
        <el-icon class="login__logo"><OfficeBuilding /></el-icon>
        <h1 class="login__title">企程 OrgFlow</h1>
        <p class="login__subtitle">公司部门通讯录与日程管理系统</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        label-position="top"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            clearable
            :prefix-icon="'User'"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            :prefix-icon="'Lock'"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="login__submit"
            :loading="loading"
            native-type="submit"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <el-divider class="login__divider">演示账号（点击可一键填入）</el-divider>

      <div class="login__demo">
        <div
          v-for="item in demoAccounts"
          :key="item.username"
          class="login__demo-item"
          @click="fillDemo(item)"
        >
          <el-tag size="small" :type="item.label === '管理员' ? 'danger' : 'info'" effect="light">
            {{ item.label }}
          </el-tag>
          <span class="login__demo-account">{{ item.username }} / {{ item.password }}</span>
        </div>
      </div>
    </el-card>

    <p class="login__footer">企程 OrgFlow · 毕业设计演示系统</p>
  </div>
</template>

<style scoped>
.login {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef3ff 0%, #f7f9fc 55%, #e8f0ff 100%);
  padding: 20px;
}

.login__card {
  width: 420px;
  max-width: 100%;
  border-radius: 12px;
  border: none;
  box-shadow: 0 12px 32px rgba(47, 111, 237, 0.12);
}

.login__header {
  text-align: center;
  margin-bottom: 18px;
}

.login__logo {
  font-size: 40px;
  color: var(--of-primary);
}

.login__title {
  margin: 8px 0 4px;
  font-size: 24px;
  font-weight: 700;
  color: var(--of-primary);
  letter-spacing: 1px;
}

.login__subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--of-text-secondary);
}

.login__submit {
  width: 100%;
}

.login__divider {
  margin: 6px 0 12px;
}

:deep(.login__divider .el-divider__text) {
  font-size: 12px;
  color: var(--of-text-secondary);
  background: #fff;
}

.login__demo {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.login__demo-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border: 1px dashed var(--of-border);
  border-radius: var(--of-radius);
  cursor: pointer;
  transition: all 0.2s;
}

.login__demo-item:hover {
  border-color: var(--of-primary);
  background: #f5f8ff;
}

.login__demo-account {
  font-size: 13px;
  color: var(--of-text-secondary);
  font-family: Consolas, Monaco, monospace;
}

.login__footer {
  margin-top: 18px;
  font-size: 12px;
  color: #9aa4b2;
}
</style>
