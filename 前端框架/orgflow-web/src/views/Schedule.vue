<script setup lang="ts">
/**
 * 日程管理
 * - 上方「公司公告」时间线（/api/schedule/announcements）
 * - 下方日程分页表格（/api/schedule/page），支持按标题、类型查询
 * - 编辑/删除按钮由记录里的 canEdit 控制显隐（管理员与发布人本人可操作）
 * - 公司公告（type=1）仅管理员可发布
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createSchedule,
  deleteSchedule,
  getSchedule,
  listAnnouncements,
  pageSchedules,
  updateSchedule,
  type ScheduleForm,
} from '@/api/schedule'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, ellipsis } from '@/utils/format'
import { SCHEDULE_ANNOUNCEMENT, SCHEDULE_PERSONAL, type Schedule } from '@/types'

const auth = useAuthStore()

/** 公告区 */
const announcements = ref<Schedule[]>([])
const announcementLoading = ref(false)

/** 查询条件 */
const query = reactive({
  title: '',
  type: null as number | null,
})

/** 分页 */
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

const tableData = ref<Schedule[]>([])
const loading = ref(false)

/** 新增/编辑弹窗 */
const dialogVisible = ref(false)
const dialogTitle = ref('新增日程')
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const submitting = ref(false)
const formRef = ref<FormInstance>()

/** 详情弹窗 */
const detailVisible = ref(false)
const detail = ref<Schedule | null>(null)

/** 当前登录用户的 user id，用于判断是否是自己的日程 */
const currentUserId = computed(() => auth.user?.id ?? null)

/** 是否可操作某条日程：优先用后端返回的 canEdit，缺失时按角色/发布人本地兜底判断 */
function canOperate(row: Schedule): boolean {
  if (typeof row.canEdit === 'boolean') return row.canEdit
  if (auth.isAdmin) return true
  return currentUserId.value !== null && row.createBy === currentUserId.value
}

const form = reactive<ScheduleForm>({
  title: '',
  content: '',
  type: SCHEDULE_PERSONAL,
  startTime: '',
  endTime: '',
})

const rules: FormRules = {
  title: [
    { required: true, message: '请输入日程标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字', trigger: 'blur' },
  ],
  type: [{ required: true, message: '请选择日程类型', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' },
    {
      validator: (_rule, value: string, callback) => {
        if (!value || !form.startTime) return callback()
        if (new Date(value.replace(' ', 'T')) < new Date(form.startTime.replace(' ', 'T'))) {
          return callback(new Error('结束时间不能早于开始时间'))
        }
        return callback()
      },
      trigger: 'change',
    },
  ],
}

/** 加载公司公告 */
async function loadAnnouncements(): Promise<void> {
  announcementLoading.value = true
  try {
    announcements.value = (await listAnnouncements()) ?? []
  } catch {
    // request.ts 已提示
  } finally {
    announcementLoading.value = false
  }
}

/** 加载日程分页 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const page = await pageSchedules({
      current: pagination.current,
      size: pagination.size,
      title: query.title || undefined,
      type: query.type ?? undefined,
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

/** 查询 */
function handleSearch(): void {
  pagination.current = 1
  void loadData()
}

/** 重置 */
function handleReset(): void {
  query.title = ''
  query.type = null
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

/** 重置表单 */
function resetForm(): void {
  form.title = ''
  form.content = ''
  // 普通员工默认「个人/部门日程」，管理员默认「公司公告」
  form.type = auth.isAdmin ? SCHEDULE_ANNOUNCEMENT : SCHEDULE_PERSONAL
  form.startTime = ''
  form.endTime = ''
}

/** 打开新增弹窗 */
function openCreate(): void {
  isEdit.value = false
  editingId.value = null
  dialogTitle.value = '新增日程'
  resetForm()
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
async function openEdit(row: Schedule): Promise<void> {
  isEdit.value = true
  editingId.value = row.id
  dialogTitle.value = '编辑日程'
  resetForm()
  try {
    const data = await getSchedule(row.id)
    fillForm(data)
  } catch {
    fillForm(row)
  }
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

function fillForm(data: Schedule): void {
  form.title = data.title ?? ''
  form.content = data.content ?? ''
  form.type = data.type ?? SCHEDULE_PERSONAL
  form.startTime = formatDateTime(data.startTime)
  form.endTime = formatDateTime(data.endTime)
}

/** 查看详情 */
async function openDetail(row: Schedule): Promise<void> {
  detail.value = row
  detailVisible.value = true
  try {
    detail.value = await getSchedule(row.id)
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
    const payload: ScheduleForm = {
      title: form.title.trim(),
      content: form.content?.trim() ?? '',
      type: form.type,
      startTime: form.startTime,
      endTime: form.endTime,
    }
    if (isEdit.value && editingId.value !== null) {
      await updateSchedule(editingId.value, payload)
      ElMessage.success('日程修改成功')
    } else {
      await createSchedule(payload)
      ElMessage.success('日程新增成功')
    }
    dialogVisible.value = false
    // 公告区与列表都刷新，保证新增公告后上方时间线同步
    await Promise.all([loadData(), loadAnnouncements()])
  } catch {
    // 403（普通员工改别人的）或参数错误由 request.ts 原样提示
  } finally {
    submitting.value = false
  }
}

/** 删除日程 */
async function handleDelete(row: Schedule): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除日程「${row.title}」吗？`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  try {
    await deleteSchedule(row.id)
    ElMessage.success('日程删除成功')
    if (tableData.value.length === 1 && pagination.current > 1) {
      pagination.current -= 1
    }
    await Promise.all([loadData(), loadAnnouncements()])
  } catch {
    // 已提示
  }
}

onMounted(async () => {
  await Promise.all([loadAnnouncements(), loadData()])
})
</script>

<template>
  <div class="page-container schedule">
    <!-- 公司公告区 -->
    <div class="of-card schedule__announcement">
      <div class="of-card__header">
        <div class="of-card__title">
          <el-icon><Bell /></el-icon>
          公司公告
          <el-tag size="small" type="warning" effect="light">{{ announcements.length }} 条</el-tag>
        </div>
        <el-button type="primary" link :icon="'Refresh'" @click="loadAnnouncements">
          刷新
        </el-button>
      </div>

      <el-skeleton v-if="announcementLoading" :rows="3" animated />
      <el-empty v-else-if="announcements.length === 0" description="暂无公司公告" :image-size="70" />
      <el-timeline v-else class="schedule__timeline">
        <el-timeline-item
          v-for="item in announcements.slice(0, 8)"
          :key="item.id"
          :timestamp="formatDateTime(item.startTime ?? item.createTime)"
          placement="top"
          type="primary"
          hollow
        >
          <div class="schedule__announcement-title">{{ item.title }}</div>
          <div class="schedule__announcement-content">{{ ellipsis(item.content, 100) || '（无内容）' }}</div>
          <div class="schedule__announcement-meta">
            <span>发布人：{{ item.createByName || '系统' }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
    </div>

    <!-- 日程列表区 -->
    <div class="of-card">
      <div class="of-card__header">
        <div class="of-card__title">
          <el-icon><Calendar /></el-icon>
          日程管理
        </div>
        <el-button type="primary" :icon="'Plus'" @click="openCreate">新增日程</el-button>
      </div>

      <div class="of-toolbar">
        <el-input
          v-model="query.title"
          placeholder="请输入日程标题"
          clearable
          class="schedule__search-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="query.type"
          placeholder="请选择类型"
          clearable
          class="schedule__search-select"
        >
          <el-option label="公司公告" :value="SCHEDULE_ANNOUNCEMENT" />
          <el-option label="个人/部门日程" :value="SCHEDULE_PERSONAL" />
        </el-select>
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
        <el-table-column label="标题" prop="title" min-width="200" show-overflow-tooltip />
        <el-table-column label="类型" prop="type" width="130" align="center">
          <template #default="{ row }">
            <el-tag
              :type="row.type === SCHEDULE_ANNOUNCEMENT ? 'warning' : 'success'"
              effect="light"
            >
              {{ row.type === SCHEDULE_ANNOUNCEMENT ? '公司公告' : '个人/部门' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" prop="startTime" min-width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.startTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="结束时间" prop="endTime" min-width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.endTime) || '—' }}</template>
        </el-table-column>
        <el-table-column label="发布人" prop="createByName" width="110" align="center">
          <template #default="{ row }">{{ row.createByName || '—' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">查看</el-button>
            <template v-if="canOperate(row)">
              <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无日程数据" :image-size="80" />
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

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="日程详情" width="560px">
      <el-descriptions v-if="detail" :column="1" border>
        <el-descriptions-item label="标题">{{ detail.title }}</el-descriptions-item>
        <el-descriptions-item label="类型">
          <el-tag
            :type="detail.type === SCHEDULE_ANNOUNCEMENT ? 'warning' : 'success'"
            effect="light"
          >
            {{ detail.type === SCHEDULE_ANNOUNCEMENT ? '公司公告' : '个人/部门日程' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatDateTime(detail.startTime) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatDateTime(detail.endTime) || '—' }}</el-descriptions-item>
        <el-descriptions-item label="发布人">{{ detail.createByName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="内容">
          <div class="schedule__detail-content">{{ detail.content || '（无内容）' }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="暂无数据" :image-size="80" />
    </el-dialog>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" maxlength="100" placeholder="请输入日程标题" />
        </el-form-item>

        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="SCHEDULE_ANNOUNCEMENT" :disabled="!auth.isAdmin">
              公司公告
            </el-radio>
            <el-radio :value="SCHEDULE_PERSONAL">个人/部门日程</el-radio>
          </el-radio-group>
          <div v-if="!auth.isAdmin" class="schedule__type-tip">
            只有管理员可以发布公司公告
          </div>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="form.startTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择开始时间"
                class="schedule__full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="form.endTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="请选择结束时间"
                class="schedule__full"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="请输入日程内容"
          />
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
.schedule {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.schedule__timeline {
  padding-left: 4px;
}

.schedule__announcement-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--of-text);
  margin-bottom: 4px;
}

.schedule__announcement-content {
  font-size: 13px;
  color: var(--of-text-secondary);
  line-height: 1.6;
}

.schedule__announcement-meta {
  margin-top: 4px;
  font-size: 12px;
  color: #9aa4b2;
}

.schedule__search-input,
.schedule__search-select {
  width: 200px;
}

.schedule__full {
  width: 100%;
}

.schedule__detail-content {
  white-space: pre-wrap;
  line-height: 1.6;
}

.schedule__type-tip {
  width: 100%;
  font-size: 12px;
  color: #e6a23c;
  line-height: 1.6;
}
</style>
