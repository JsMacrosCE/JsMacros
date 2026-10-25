import DefaultTheme from 'vitepress/theme'
import BrowserSidebarLayout from './BrowserSidebarLayout.vue'
import MinecraftVersionSelector from './MinecraftVersionSelector.vue'
import './style.css'

export default {
  extends: DefaultTheme,
  Layout: BrowserSidebarLayout,
  enhanceApp({ app }) {
    app.component('MinecraftVersionSelector', MinecraftVersionSelector)
  }
} satisfies import('vitepress').Theme
