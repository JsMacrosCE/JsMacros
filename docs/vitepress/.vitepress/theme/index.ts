import DefaultTheme from 'vitepress/theme'
import ApiReferenceLink from './ApiReferenceLink.vue'
import BrowserSidebarLayout from './BrowserSidebarLayout.vue'
import MinecraftVersionSelector from './MinecraftVersionSelector.vue'
import './style.css'

export default {
  extends: DefaultTheme,
  Layout: BrowserSidebarLayout,
  enhanceApp({ app }) {
    app.component('ApiReferenceLink', ApiReferenceLink)
    app.component('MinecraftVersionSelector', MinecraftVersionSelector)
  }
} satisfies import('vitepress').Theme
