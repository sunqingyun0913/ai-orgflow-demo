/**
 * Element Plus 表格类型补充
 *
 * el-table 的插槽 row 类型是 TableColumnCtx，它只声明了后端返回字段里的一部分。
 * 我们的列表行会带一些业务字段（例如部门的 employeeCount、员工的 departmentName），
 * 这里给 TableColumnCtx 加一个索引签名，让自定义 prop 也能通过 vue-tsc 检查。
 */
import 'element-plus'

declare module 'element-plus' {
  interface TableColumnCtx<T> {
    [key: string]: any
  }
}
