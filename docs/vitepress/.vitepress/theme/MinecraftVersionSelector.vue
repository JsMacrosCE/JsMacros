<script setup lang="ts">
import { useData } from 'vitepress'
import VPNavBarMenuGroup from 'vitepress/dist/client/theme-default/components/VPNavBarMenuGroup.vue'
import VPNavScreenMenuGroup from 'vitepress/dist/client/theme-default/components/VPNavScreenMenuGroup.vue'
import type { DefaultTheme } from 'vitepress/theme'
import { computed } from 'vue'
import { selectedVersion } from './target-selector'

const props = defineProps<{
  items: (DefaultTheme.NavItemChildren & { items: { text: string; link: string }[] })[]
  fallbackVersion: string
  screenMenu?: boolean
}>()
const { page } = useData()
const text = computed(() => selectedVersion(page.value.relativePath, props.items, props.fallbackVersion))
const item = computed(() => ({ text: text.value, items: props.items }))
</script>

<template>
  <VPNavScreenMenuGroup v-if="screenMenu" :text="text" :items="items" />
  <VPNavBarMenuGroup v-else :item="item" />
</template>
