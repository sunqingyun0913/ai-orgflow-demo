/**
 * @element-plus/icons-vue 图标集合
 * 以「图标名 → 组件」的对象形式导出，便于在菜单配置里用图标名引用，
 * 同时统一注册到全局，模板里可以直接用 <el-icon><HomeFilled /></el-icon>。
 */
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import type { App, Component } from 'vue'

/** 全部图标：{ HomeFilled: Component, ... } */
export const icons = ElementPlusIconsVue as unknown as Record<string, Component>

/** 把全部 Element Plus 图标注册为全局组件 */
export function registerIcons(app: App): void {
  for (const [name, component] of Object.entries(icons)) {
    app.component(name, component)
  }
}
