<script setup lang="ts">
/**
 * 用户管理（仅管理员，路由 meta.roles: [1] 已做拦截）
 * - 分页查询 /api/user/page
 * - 新增 / 修改 / 启用禁用 / 删除
 * - 账号与员工档案通过 employeeId 关联，员工下拉来自 /api/employee/options
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createUser,
  deleteUser,
  pageUsers,
  updateUser,
  updateUserStatus,
} from '@/api/user'
import { listEmployeeOptions } from '@/api/employee'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime } from '@/utils/format'
import {
  ACCOUNT_DISABLED,
  ACCOUNT_ENABLED,
  ROLE_ADMIN,
  ROLE_EMPLOYEE,
  type EmployeeOption,
  type UserRecord,
} from '@/types'

const auth = useAuthStore()

/** 查询条件 */
const query = reactive({ username: '' })

/** 分页 */
const pagination = reactive({ current: 1, size: 10, total: 0 })

const tableData = ref<UserRecord[]>([])
const loading = ref(false)
/** 正在切换状态的用户 id（避免重复点击） */
const statusChangingId = ref<number | null>(null)

/** 员工下拉选项 */
const employeeOptions = ref<EmployeeOption[]>([])

/** 新增/编辑弹窗 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  password: '',
  employeeId: null as number | null,
  role: ROLE_EMPLOYEE,
  status: ACCOUNT_ENABLED,
})

/** 密码规则：新增必填且 ≥6 位；编辑时留空表示不修改 */
const rules: FormRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 20, message: '账号长度需在 3 到 20 个字符之间', trigger: 'blur' },
  ],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (isEdit.value && !value) return callback()
        if (!value) return callback(new Error('请输入密码'))
        if (value.length < 6) return callback(new Error('密码长度不能少于 6 位'))
        return callback()
      },
      trigger: 'blur',
    },
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

/** 加载用户分页 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const page = await pageUsers({
      current: pagination.current,
      size: pagination.size,
      username: query.username || undefined,
    })
    tableData.value = page?.records ?? []
    pagination.total = page?.total ?? 0
    pagination.current = page?.current ?? pagination.current
  } catch {
    // request.ts 已提示
  } finally {
    loading.value = false
  }
}

/** 加载员工下拉选项 */
async function loadEmployees(): Promise<void> {
  try {
    employeeOptions.value = (await listEmployeeOptions()) ?? []
  } catch {
    // 下拉为空不影响主流程
  }
}

function handleSearch(): void {
  pagination.current = 1
  void loadData()
}

function handleReset(): void {
  query.username = ''
  pagination.current = 1
  void loadData()
}

function handlePageChange(page: number): void {
  pagination.current = page
  void loadData()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
  void loadData()
}

/** 打开新增弹窗 */
function openCreate(): void {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增用户'
  form.username = ''
  form.password = ''
  form.employeeId = null
  form.role = ROLE_EMPLOYEE
  form.status = ACCOUNT_ENABLED
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEdit(row: UserRecord): void {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑用户'
  form.username = row.username
  form.password = ''
  form.employeeId = row.employeeId ?? null
  form.role = row.role ?? ROLE_EMPLOYEE
  form.status = row.status ?? ACCOUNT_ENABLED
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 提交新增/编辑 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && editingId.value !== null) {
      await updateUser(editingId.value, {
        // 留空表示不修改密码（契约第 4.5 节）
        password: form.password || undefined,
        employeeId: form.employeeId,
        role: form.role,
        status: form.status,
      })
      ElMessage.success('用户修改成功')
    } else {
      await createUser({
        username: form.username.trim(),
        password: form.password,
        employeeId: form.employeeId,
        role: form.role,
        status: form.status,
      })
      ElMessage.success('用户新增成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch {
    // 账号已存在等后端 message 由 request.ts 原样提示
  } finally {
    submitting.value = false
  }
}

/** 切换启用/禁用 */
async function handleStatusChange(row: UserRecord): Promise<void> {
  statusChangingId.value = row.id
  try {
    await updateUserStatus(row.id, row.status ?? ACCOUNT_ENABLED)
    ElMessage.success(row.status === ACCOUNT_ENABLED ? '账号已启用' : '账号已禁用')
  } catch {
    // 失败时把开关恢复原状
    row.status = row.status === ACCOUNT_ENABLED ? ACCOUNT_DISABLED : ACCOUNT_ENABLED
  } finally {
    statusChangingId.value = null
  }
}

/** 删除用户 */
async function handleDelete(row: UserRecord): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteUser(row.id)
    ElMessage.success('用户删除成功')
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    await loadData()
  } catch {
    // 不能删除自己等后端 message 由 request.ts 原样提示
  }
}

onMounted(async () => {
  await loadEmployees()
  await loadData()
})
</script>

<template>
  <div class="page-container user">
    <div class="of-card">
      <div class="of-card__header">
        <div class="of-card__title">
          <el-icon><Setting /></el-icon>
          用户管理
        </div>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增用户</el-button>
      </div>

      <div class="of-toolbar">
        <el-input
          v-model="query.username"
          placeholder="请输入账号"
          clearable
          class="user__search-input"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        :header-cell-style="{ background: '#fafbfc' }"
      >
        <el-table-column label="账号" prop="username" min-width="130" fixed="left" />
        <el-table-column label="关联员工" prop="employeeName" min-width="130">
          <template #default="{ row }">
            <span>{{ row.employeeName || row.employee?.name || '未关联' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="角色" prop="role" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === ROLE_ADMIN ? 'danger' : 'info'" effect="light">
              {{ row.role === ROLE_ADMIN ? '管理员' : '普通员工' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="ACCOUNT_ENABLED"
              :inactive-value="ACCOUNT_DISABLED"
              :loading="statusChangingId === row.id"
              :disabled="row.id === auth.user?.id"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button
              type="danger"
              link
              :disabled="row.id === auth.user?.id"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无用户数据" :image-size="80" />
        </template>
      </el-table>

      <div class="of-pagination">
        <el-pagination
          :current-page="pagination.current"
          :page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="账号" prop="username">
          <el-input
            v-model="form.username"
            maxlength="20"
            :disabled="isEdit"
            placeholder="请输入登录账号"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="isEdit ? '留空表示不修改密码' : '至少 6 位'"
          />
        </el-form-item>

        <el-form-item label="关联员工" prop="employeeId">
          <el-select
            v-model="form.employeeId"
            filterable
            clearable
            placeholder="请选择关联员工"
            class="user__full"
          >
            <el-option
              v-for="item in employeeOptions"
              :key="item.id"
              :label="item.departmentName ? `${item.name}（${item.departmentName}）` : item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="form.role">
            <el-radio :value="ROLE_EMPLOYEE">普通员工</el-radio>
            <el-radio :value="ROLE_ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="ACCOUNT_ENABLED">启用</el-radio>
            <el-radio :value="ACCOUNT_DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user__search-input {
  width: 220px;
}

.user__full {
  width: 100%;
}
</style>
