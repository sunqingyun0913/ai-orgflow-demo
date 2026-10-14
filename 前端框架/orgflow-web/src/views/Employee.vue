<script setup lang="ts">
/**
 * 通讯录（员工管理）
 * - 搜索：姓名 / 部门 / 状态
 * - el-table 分页展示 /api/employee/page
 * - 新增 / 编辑 / 删除仅管理员，普通员工只能查看
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createEmployee,
  deleteEmployee,
  getEmployee,
  pageEmployees,
  updateEmployee,
  type EmployeeForm,
} from '@/api/employee'
import { getDepartmentTree } from '@/api/department'
import { treeSelectProps } from '@/utils/tree'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/format'
import { EMPLOYEE_ACTIVE, EMPLOYEE_RESIGNED, type DepartmentTreeNode, type Employee } from '@/types' // prettier-ignore

const auth = useAuthStore()

/** 查询条件 */
const query = reactive({
  name: '',
  departmentId: null as number | null,
  status: null as number | null,
})

/** 分页 */
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

const tableData = ref<Employee[]>([])
const loading = ref(false)

/** 部门下拉树 */
const departmentTree = ref<DepartmentTreeNode[]>([])
/** 部门下拉：加一个「全部/未分配」方便清空 */
const departmentOptions = ref<DepartmentTreeNode[]>([])

/** 新增/编辑弹窗 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增员工')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

/** 查看详情抽屉 */
const drawerVisible = ref(false)
const detail = ref<Employee | null>(null)

const form = reactive<EmployeeForm>({
  name: '',
  departmentId: null,
  phone: '',
  email: '',
  position: '',
  entryDate: '',
  status: EMPLOYEE_ACTIVE,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入员工姓名', trigger: 'blur' },
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

/** 加载员工分页 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const page = await pageEmployees({
      current: pagination.current,
      size: pagination.size,
      name: query.name || undefined,
      departmentId: query.departmentId ?? undefined,
      status: query.status ?? undefined,
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

/** 加载部门下拉 */
async function loadDepartments(): Promise<void> {
  try {
    const tree = await getDepartmentTree()
    departmentTree.value = tree ?? []
    departmentOptions.value = tree ?? []
  } catch {
    // 忽略，部门下拉为空不影响主流程
  }
}

/** 查询 */
function handleSearch(): void {
  pagination.current = 1
  void loadData()
}

/** 重置查询条件 */
function handleReset(): void {
  query.name = ''
  query.departmentId = null
  query.status = null
  pagination.current = 1
  void loadData()
}

/** 页码/每页条数变化 */
function handlePageChange(page: number): void {
  pagination.current = page
  void loadData()
}

function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
  void loadData()
}

/** 重置表单 */
function resetForm(): void {
  form.name = ''
  form.departmentId = null
  form.phone = ''
  form.email = ''
  form.position = ''
  form.entryDate = ''
  form.status = EMPLOYEE_ACTIVE
}

/** 打开新增弹窗 */
function openCreate(): void {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增员工'
  resetForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 打开编辑弹窗（用详情接口拿最新数据） */
async function openEdit(row: Employee): Promise<void> {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑员工'
  resetForm()
  try {
    const data = await getEmployee(row.id)
    form.name = data.name ?? ''
    form.departmentId = data.departmentId ?? null
    form.phone = data.phone ?? ''
    form.email = data.email ?? ''
    form.position = data.position ?? ''
    form.entryDate = data.entryDate ? formatDate(data.entryDate) : ''
    form.status = data.status ?? EMPLOYEE_ACTIVE
  } catch {
    // 详情拿不到时退回用列表行数据
    form.name = row.name ?? ''
    form.departmentId = row.departmentId ?? null
    form.phone = row.phone ?? ''
    form.email = row.email ?? ''
    form.position = row.position ?? ''
    form.entryDate = row.entryDate ? formatDate(row.entryDate) : ''
    form.status = row.status ?? EMPLOYEE_ACTIVE
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 查看详情 */
async function openDetail(row: Employee): Promise<void> {
  detail.value = row
  drawerVisible.value = true
  try {
    detail.value = await getEmployee(row.id)
  } catch {
    // 保留列表行数据
  }
}

/** 提交新增/编辑 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload: EmployeeForm = {
      name: form.name.trim(),
      departmentId: form.departmentId,
      phone: form.phone?.trim() ?? '',
      email: form.email?.trim() ?? '',
      position: form.position?.trim() ?? '',
      entryDate: form.entryDate || '',
      status: form.status,
    }
    if (isEdit.value && editingId.value !== null) {
      await updateEmployee(editingId.value, payload)
      ElMessage.success('员工信息修改成功')
    } else {
      await createEmployee(payload)
      ElMessage.success('员工新增成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch {
    // 后端校验错误（姓名重复、手机号格式等）由 request.ts 原样提示
  } finally {
    submitting.value = false
  }
}

/** 删除员工 */
async function handleDelete(row: Employee): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除员工「${row.name}」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteEmployee(row.id)
    ElMessage.success('员工删除成功')
    // 删掉当前页最后一条时回退一页
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    await loadData()
  } catch {
    // 已提示
  }
}

onMounted(async () => {
  await loadDepartments()
  await loadData()
})
</script>

<template>
  <div class="page-container employee">
    <div class="of-card">
      <div class="of-card__header">
        <div class="of-card__title">
          <el-icon><User /></el-icon>
          通讯录
        </div>
        <el-button v-if="auth.isAdmin" type="primary" :icon="'Plus'" @click="openCreate">
          新增员工
        </el-button>
      </div>

      <!-- 搜索区 -->
      <div class="of-toolbar">
        <el-input
          v-model="query.name"
          placeholder="请输入员工姓名"
          clearable
          class="employee__search-input"
          @keyup.enter="handleSearch"
        />
        <el-tree-select
          v-model="query.departmentId"
          :data="departmentOptions"
          :props="treeSelectProps"
          node-key="id"
          check-strictly
          clearable
          default-expand-all
          :render-after-expand="false"
          placeholder="请选择部门"
          class="employee__search-select"
        />
        <el-select
          v-model="query.status"
          placeholder="请选择状态"
          clearable
          class="employee__search-select"
        >
          <el-option label="在职" :value="EMPLOYEE_ACTIVE" />
          <el-option label="离职" :value="EMPLOYEE_RESIGNED" />
        </el-select>
        <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        <el-button :icon="'Refresh'" @click="handleReset">重置</el-button>
      </div>

      <!-- 表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        border
        stripe
        :header-cell-style="{ background: '#fafbfc' }"
      >
        <el-table-column label="姓名" prop="name" min-width="110" fixed="left" />
        <el-table-column label="部门" prop="departmentName" min-width="140">
          <template #default="{ row }">
            <span>{{ row.departmentName || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="职位" prop="position" min-width="130">
          <template #default="{ row }">
            <span>{{ row.position || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="手机号" prop="phone" min-width="130">
          <template #default="{ row }">
            <span>{{ row.phone || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" prop="email" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.email || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="入职日期" prop="entryDate" min-width="120" align="center">
          <template #default="{ row }">
            <span>{{ formatDate(row.entryDate) || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === EMPLOYEE_ACTIVE ? 'success' : 'info'" effect="light">
              {{ row.status === EMPLOYEE_ACTIVE ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <template v-if="auth.isAdmin">
              <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无员工数据" :image-size="80" />
        </template>
      </el-table>

      <!-- 分页 -->
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

    <!-- 详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="员工详情" size="420px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="姓名">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="部门">
          {{ detail.departmentName || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="职位">{{ detail.position || '—' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail.phone || '—' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail.email || '—' }}</el-descriptions-item>
        <el-descriptions-item label="入职日期">
          {{ formatDate(detail.entryDate) || '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === EMPLOYEE_ACTIVE ? 'success' : 'info'" effect="light">
            {{ detail.status === EMPLOYEE_ACTIVE ? '在职' : '离职' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-drawer>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" maxlength="20" placeholder="请输入员工姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门" prop="departmentId">
              <el-tree-select
                v-model="form.departmentId"
                :data="departmentTree"
                :props="treeSelectProps"
                node-key="id"
                check-strictly
                clearable
                default-expand-all
                :render-after-expand="false"
                placeholder="请选择部门"
                class="employee__full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职位" prop="position">
              <el-input v-model="form.position" maxlength="50" placeholder="请输入职位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入职日期" prop="entryDate">
              <el-date-picker
                v-model="form.entryDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择入职日期"
                class="employee__full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="form.status">
                <el-radio :value="EMPLOYEE_ACTIVE">在职</el-radio>
                <el-radio :value="EMPLOYEE_RESIGNED">离职</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.employee__search-input {
  width: 200px;
}

.employee__search-select {
  width: 200px;
}

.employee__full {
  width: 100%;
}
</style>
