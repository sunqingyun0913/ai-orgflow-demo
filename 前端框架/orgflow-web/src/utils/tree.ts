/**
 * el-tree / el-tree-select 的公用字段映射配置。
 *
 * 契约里的部门树字段名是 id / name / children，需要显式告诉 Element Plus 用哪几个字段。
 * 注意：Element Plus 的 `props`（TreeOptionProps）只接受 label / children / disabled / isLeaf，
 * 节点的取值字段由 `node-key` 指定（项目里统一写 node-key="id"），所以这里不能写 value —— 写了
 * 既不会被使用，也会让 vue-tsc 报 TS2353。
 */
import type { TreeOptionProps } from 'element-plus'

/** 部门树 / 部门下拉的字段映射，配合 node-key="id" 使用 */
export const treeSelectProps: TreeOptionProps = {
  label: 'name',
  children: 'children',
}
