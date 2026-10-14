<script setup lang="ts">
/**
 * 个人中心
 * - 左侧：当前登录用户信息（账号、姓名、部门、职位、手机号、邮箱、入职日期、角色）
 * - 右侧：修改密码表单，调 PUT /api/auth/password，成功后退出登录并跳登录页
 *
 * 说明：GET /api/auth/info 只返回简单的 employee 信息（无 phone/email/entryDate），
 * 所以这里再用 employeeId 调一次 GET /api/employee/{id} 把完整资料补全。
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { updatePassword } from '@/api/auth'
import { getEmployee, updateEmployee } from '@/api/employee'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'
import { ROLE_ADMIN, type Employee } from '@/types'

const router = useRouter()
const auth = useAuthStore()

/** 完整员工资料（可能拿不到） */
const employeeDetail = ref<Employee | null>(null)

/** 显示用的合并信息 */
const profile = computed(() => ({
  username: auth.user?.username ?? '—',
  name: auth.employee?.name ?? '—',
  departmentName: auth.employee?.departmentName ?? '—',
  position: auth.employee?.position ?? '—',
  phone: employeeDetail.value?.phone ?? '—',
  email: employeeDetail.value?.email ?? '—',
  entryDate: employeeDetail.value?.entryDate
    ? formatDate(employeeDetail.value.entryDate)
    : '—',
  role: auth.isAdmin ? '管理员' : '普通员工',
}))

/** 当前用户是否管理员（员工资料修改仅管理员有权限） */
const isAdmin = computed(() => auth.user?.role === ROLE_ADMIN)

/** 修改资料弹窗 */
const editVisible = ref(false)
const editSubmitting = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive({
  name: '',
  position: '',
  phone: '',
  email: '',
})

const editRules: FormRules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 20, message: '姓名不能超过 20 个字', trigger: 'blur' },
  ],
  phone: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) return callback()
        if (!/^\d{11}$/.test(value)) return callback(new Error('手机号必须是 11 位数字'))
        return callback()
      },
      trigger: 'blur',
    },
  ],
  email: [
    {
      validator: (_rule, value: string, callback) => {
        if (!value) return callback()
        if (!value.includes('@')) return callback(new Error('邮箱格式不正确，必须包含 @'))
        return callback()
      },
      trigger: 'blur',
    },
  ],
}

/** 修改密码表单 */
const pwdFormRef = ref<FormInstance>()
const pwdSubmitting = ref(false)
const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value: string, callback) => {
        if (value !== pwdForm.newPassword) {
          return callback(new Error('两次输入的新密码不一致'))
        }
        return callback()
      },
      trigger: 'blur',
    },
  ],
}

/** 加载完整员工资料 */
async function loadEmployeeDetail(): Promise<void> {
  const employeeId = auth.user?.employeeId ?? auth.employee?.id
  if (!employeeId) return
  try {
    employeeDetail.value = await getEmployee(employeeId)
  } catch {
    // 拿不到就只展示 auth store 里的简要信息
  }
}

/** 打开修改资料弹窗 */
function openEdit(): void {
  editForm.name = auth.employee?.name ?? ''
  editForm.position = auth.employee?.position ?? ''
  editForm.phone = employeeDetail.value?.phone ?? ''
  editForm.email = employeeDetail.value?.email ?? ''
  editFormRef.value?.clearValidate()
  editVisible.value = true
}

/** 提交资料修改 */
async function submitEdit(): Promise<void> {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return

  const employeeId = auth.user?.employeeId ?? auth.employee?.id
  if (!employeeId) {
    ElMessage.warning('当前账号未关联员工信息，无法修改资料')
    return
  }

  editSubmitting.value = true
  try {
    const updated = await updateEmployee(employeeId, {
      name: editForm.name.trim(),
      // 部门不在个人中心修改，保持原值
      departmentId: employeeDetail.value?.departmentId ?? auth.employee?.departmentId ?? null,
      phone: editForm.phone.trim(),
      email: editForm.email.trim(),
      position: editForm.position.trim(),
      entryDate: employeeDetail.value?.entryDate ?? '',
      status: employeeDetail.value?.status ?? 1,
    })
    employeeDetail.value = updated
    // 同步顶部栏与欢迎语里的姓名/职位
    auth.setAuth({
      employee: {
        id: employeeId,
        name: updated.name ?? editForm.name.trim(),
        position: updated.position ?? editForm.position.trim(),
        departmentId: updated.departmentId ?? null,
        departmentName: updated.departmentName ?? auth.employee?.departmentName ?? null,
      },
    })
    ElMessage.success('个人资料修改成功')
    editVisible.value = false
  } catch {
    // 后端 message 已由 request.ts 提示
  } finally {
    editSubmitting.value = false
  }
}

/** 提交修改密码 */
async function submitPassword(): Promise<void> {
  if (!pwdFormRef.value) return
  const valid = await pwdFormRef.value.validate().catch(() => false)
  if (!valid) return

  pwdSubmitting.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    // 密码已变更，退出登录并回登录页
    await auth.logout()
    await router.push('/login')
  } catch {
    // 原密码错误等由 request.ts 原样提示
  } finally {
    pwdSubmitting.value = false
  }
}

onMounted(async () => {
  if (auth.token && !auth.user) {
    try {
      await auth.fetchInfo()
    } catch {
      // 401 时 request.ts 已跳登录页
    }
  }
  await loadEmployeeDetail()
})
</script>

<template>
  <div class="page-container profile">
    <el-row :gutter="16">
      <!-- 左：个人信息 -->
      <el-col :xs="24" :md="14">
        <div class="of-card profile__card">
          <div class="of-card__header">
            <div class="of-card__title">
              <el-icon><UserFilled /></el-icon>
              我的信息
            </div>
            <el-button
              v-if="isAdmin && auth.user?.employeeId"
              type="primary"
              link
              :icon="'Edit'"
              @click="openEdit"
            >
              修改资料
            </el-button>
          </div>

          <div class="profile__head">
            <el-avatar :size="56" class="profile__avatar">
              {{ profile.name.charAt(0) }}
            </el-avatar>
            <div class="profile__head-text">
              <div class="profile__name">{{ profile.name }}</div>
              <div class="profile__sub">
                {{ profile.departmentName }} · {{ profile.position }}
              </div>
            </div>
            <el-tag :type="auth.isAdmin ? 'danger' : 'info'" effect="light">
              {{ profile.role }}
            </el-tag>
          </div>

          <el-descriptions :column="1" border class="profile__desc">
            <el-descriptions-item label="账号">{{ profile.username }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ profile.name }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ profile.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ profile.position }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ profile.phone }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ profile.email }}</el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ profile.entryDate }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ profile.role }}</el-descriptions-item>
          </el-descriptions>

          <el-alert
            v-if="!auth.user?.employeeId"
            class="profile__alert"
            type="info"
            :closable="false"
            show-icon
            title="当前账号未关联员工档案，员工相关信息显示为空"
          />
        </div>
      </el-col>

      <!-- 右：修改密码 -->
      <el-col :xs="24" :md="10">
        <div class="of-card profile__card">
          <div class="of-card__header">
            <div class="of-card__title">
              <el-icon><Lock /></el-icon>
              修改密码
            </div>
          </div>

          <el-form
            ref="pwdFormRef"
            :model="pwdForm"
            :rules="pwdRules"
            label-width="96px"
            @submit.prevent="submitPassword"
          >
            <el-form-item label="原密码" prop="oldPassword">
              <el-input
                v-model="pwdForm.oldPassword"
                type="password"
                show-password
                placeholder="请输入原密码"
                @keyup.enter="submitPassword"
              />
            </el-form-item>

            <el-form-item label="新密码" prop="newPassword">
              <el-input
                v-model="pwdForm.newPassword"
                type="password"
                show-password
                placeholder="至少 6 位"
                @keyup.enter="submitPassword"
              />
            </el-form-item>

            <el-form-item label="确认新密码" prop="confirmPassword">
              <el-input
                v-model="pwdForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入新密码"
                @keyup.enter="submitPassword"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="pwdSubmitting" @click="submitPassword">
                确认修改
              </el-button>
              <el-button @click="pwdFormRef?.resetFields()">重置</el-button>
            </el-form-item>
          </el-form>

          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="修改密码成功后需要重新登录"
          />
        </div>
      </el-col>
    </el-row>

    <!-- 修改资料弹窗 -->
    <el-dialog v-model="editVisible" title="修改个人资料" width="480px" destroy-on-close>
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="editForm.name" maxlength="20" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="职位" prop="position">
          <el-input v-model="editForm.position" maxlength="50" placeholder="请输入职位" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="editForm.phone" maxlength="11" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile__card {
  margin-bottom: 16px;
}

.profile__head {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 16px;
  margin-bottom: 16px;
  border-bottom: 1px dashed var(--of-border);
}

.profile__avatar {
  background: var(--of-primary);
  color: #fff;
  font-size: 22px;
  flex-shrink: 0;
}

.profile__head-text {
  flex: 1;
  min-width: 0;
}

.profile__name {
  font-size: 18px;
  font-weight: 700;
  color: var(--of-text);
}

.profile__sub {
  margin-top: 4px;
  font-size: 13px;
  color: var(--of-text-secondary);
}

.profile__alert {
  margin-top: 14px;
}
</style>
