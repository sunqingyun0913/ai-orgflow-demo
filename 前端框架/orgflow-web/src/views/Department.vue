<script setup lang="ts">
/**
 * 部门管理
 * - 树形表格展示组织架构（/api/department/tree）
 * - 用 /api/department/list 补上 employeeCount
 * - 新增 / 编辑 / 删除仅管理员可见可操作，普通员工只读
 */
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  createDepartment,
  deleteDepartment,
  listDepartments,
  getDepartmentTree,
  updateDepartment,
} from '@/api/department'
import { treeSelectProps } from '@/utils/tree'
import { useAuthStore } from '@/stores/auth'
import type { Department, DepartmentTreeNode } from '@/types'

const auth = useAuthStore()

/** 表格数据：tree 与 list 按 id 合并（补 employeeCount、createTime） */
const tableData = ref<DepartmentTreeNode[]>([])
const loading = ref(false)
const submitting = ref(false)

/** 上级部门可选项：顶级部门(id=0) + 部门树 */
const parentTree = ref<DepartmentTreeNode[]>([])

/** 新增/编辑弹窗 */
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  parentId: 0,
  sortOrder: 0,
})

const rules: FormRules = {
  name: [
    { required: true, message: '请输入部门名称', trigger: 'blur' },
    { max: 50, message: '部门名称不能超过 50 个字', trigger: 'blur' },
  ],
}

/** 上级部门下拉的树：第一项是「顶级部门」
 * 编辑时把自己和自己的子孙节点剔除，避免把上级部门设成自己或自己的子孙（契约第 4.2 节约束）。
 */
const parentOptions = computed<DepartmentTreeNode[]>(() => {
  let nodes = parentTree.value

  if (isEdit.value && editingId.value !== null) {
    const self = findNode(parentTree.value, editingId.value)
    if (self) {
      // 禁用自己与全部子孙，保留结构但不可选
      const forbidden = new Set<number>([self.id, ...collectDescendantIds(self)])
      const mark = (list: DepartmentTreeNode[]): DepartmentTreeNode[] =>
        list.map((node) => ({
          ...node,
          disabled: forbidden.has(node.id) ? true : undefined,
          children: node.children?.length ? mark(node.children) : node.children,
        }))
      nodes = mark(nodes)
    }
  }

  return [{ id: 0, name: '顶级部门', parentId: -1, children: nodes }]
})

/** 收集某个节点的所有子孙 id（编辑时不能把上级部门设成自己或子孙） */
function collectDescendantIds(node: DepartmentTreeNode): number[] {
  const ids: number[] = []
  const walk = (list: DepartmentTreeNode[]): void => {
    for (const item of list) {
      ids.push(item.id)
      if (item.children?.length) walk(item.children)
    }
  }
  if (node.children?.length) walk(node.children)
  return ids
}

/**
 * 加载数据：tree 与 list 合并
 * tree 提供层级结构（children），list 提供 employeeCount、createTime 等字段。
 */
async function loadData(): Promise<void> {
  loading.value = true
  try {
    const [tree, list] = await Promise.all([getDepartmentTree(), listDepartments()])
    const map = new Map<number, Department>()
    for (const item of list ?? []) map.set(item.id, item)

    const merge = (nodes: DepartmentTreeNode[]): DepartmentTreeNode[] =>
      nodes
        .slice()
        // 后端已按 sort_order,id 排序；这里再兜一层，防止层级内顺序异常
        .sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.id - b.id)
        .map((node) => {
          const extra = map.get(node.id)
          return {
            ...node,
            sortOrder: node.sortOrder ?? extra?.sortOrder ?? 0,
            employeeCount: extra?.employeeCount ?? 0,
            createTime: extra?.createTime,
            children: node.children?.length ? merge(node.children) : undefined,
          } as DepartmentTreeNode
        })

    tableData.value = merge(tree ?? [])
    parentTree.value = tree ?? []
  } catch {
    // request.ts 已提示错误
  } finally {
    loading.value = false
  }
}

/** 打开新增弹窗 */
function openCreate(): void {
  isEdit.value = false
  editingId.value = null
  form.name = ''
  form.parentId = 0
  form.sortOrder = 0
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 打开编辑弹窗 */
function openEdit(row: DepartmentTreeNode): void {
  isEdit.value = true
  editingId.value = row.id
  form.name = row.name
  form.parentId = row.parentId ?? 0
  form.sortOrder = row.sortOrder ?? 0
  formRef.value?.clearValidate()
  dialogVisible.value = true
}

/** 在树里按 id 找节点 */
function findNode(nodes: DepartmentTreeNode[], id: number): DepartmentTreeNode | null {
  for (const node of nodes) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 提交新增/编辑 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      name: form.name.trim(),
      parentId: form.parentId ?? 0,
      sortOrder: form.sortOrder ?? 0,
    }
    if (isEdit.value && editingId.value !== null) {
      await updateDepartment(editingId.value, payload)
      ElMessage.success('部门修改成功')
    } else {
      await createDepartment(payload)
      ElMessage.success('部门新增成功')
    }
    dialogVisible.value = false
    await loadData()
  } catch {
    // 后端 message（例如「部门名已存在」）由 request.ts 原样弹出
  } finally {
    submitting.value = false
  }
}

/** 删除部门 */
async function handleDelete(row: DepartmentTreeNode): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除部门「${row.name}」吗？该部门下若还有员工将无法删除。`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }

  try {
    await deleteDepartment(row.id)
    ElMessage.success('部门删除成功')
    await loadData()
  } catch {
    // 契约第 4.2 节：该部门下还有员工时后端返回 400「该部门下还有员工，无法删除」，
    // request.ts 已经用 ElMessage.error 原样显示后端 message，这里无需再包一层
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container department">
    <div class="of-card">
      <div class="of-card__header">
        <div class="of-card__title">
          <el-icon><OfficeBuilding /></el-icon>
          组织架构
        </div>
        <div class="department__actions">
          <el-tag v-if="!auth.isAdmin" type="info" effect="plain">
            当前为只读模式，仅管理员可维护部门
          </el-tag>
          <el-button v-if="auth.isAdmin" type="primary" :icon="'Plus'" @click="openCreate">
            新增部门
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        default-expand-all
        :tree-props="{ children: 'children' }"
        border
        stripe
        :header-cell-style="{ background: '#fafbfc' }"
      >
        <el-table-column label="部门名称" prop="name" min-width="280">
          <template #default="{ row }">
            <span class="department__name">
              <el-icon v-if="row.parentId !== 0" class="department__name-icon">
                <Document />
              </el-icon>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="部门人数" prop="employeeCount" width="120" align="center">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.employeeCount ?? 0 }} 人</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="上级部门" width="160" align="center">
          <template #default="{ row }">
            <span v-if="row.parentId === 0" class="department__top">顶级部门</span>
            <span v-else>{{ findNode(tableData, row.parentId)?.name ?? '—' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="排序号" prop="sortOrder" width="100" align="center" />

        <el-table-column v-if="auth.isAdmin" label="操作" width="160" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无部门数据" :image-size="80" />
        </template>
      </el-table>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑部门' : '新增部门'"
      width="460px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="88px">
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="form.parentId"
            :data="parentOptions"
            :props="treeSelectProps"
            node-key="id"
            check-strictly
            :render-after-expand="false"
            default-expand-all
            placeholder="请选择上级部门"
            class="department__tree-select"
          />
        </el-form-item>

        <el-form-item label="部门名称" prop="name">
          <el-input
            v-model="form.name"
            maxlength="50"
            show-word-limit
            placeholder="请输入部门名称"
          />
        </el-form-item>

        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" />
          <span class="department__tip">数字越小越靠前</span>
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
.department__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.department__name {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 500;
}

.department__name-icon {
  color: var(--of-text-secondary);
  font-size: 13px;
}

.department__top {
  color: var(--of-text-secondary);
}

.department__tree-select {
  width: 100%;
}

.department__tip {
  margin-left: 10px;
  font-size: 12px;
  color: var(--of-text-secondary);
}
</style>
