import DefaultTheme from 'vitepress/theme'
import MinecraftVersionSelector from './MinecraftVersionSelector.vue'
import './style.css'

export default {
  extends: DefaultTheme,
  enhanceApp({ app }) {
    app.component('MinecraftVersionSelector', MinecraftVersionSelector)
  }
} satisfies import('vitepress').Theme
