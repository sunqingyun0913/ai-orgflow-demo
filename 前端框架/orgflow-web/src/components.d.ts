/**
 * 全局组件类型声明
 *
 * main.ts 里用 app.component() 注册了全部 @element-plus/icons-vue 图标，
 * 但那种运行时注册不会产生类型信息，vue-tsc 会把模板里的 <OfficeBuilding /> 当成未知组件。
 * 这里用 GlobalComponents 接口补齐类型；Element Plus 的 el-* 组件由库自带类型提供。
 */
import type * as ElementPlusIconsVue from '@element-plus/icons-vue'

declare module 'vue' {
  export interface GlobalComponents extends ElementPlusIconsVue.GlobalComponents {}
}
