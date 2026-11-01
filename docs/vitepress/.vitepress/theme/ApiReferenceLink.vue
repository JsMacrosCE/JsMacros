<script setup lang="ts">
import { useData } from 'vitepress'
import VPNavBarMenuLink from 'vitepress/dist/client/theme-default/components/VPNavBarMenuLink.vue'
import VPNavScreenMenuLink from 'vitepress/dist/client/theme-default/components/VPNavScreenMenuLink.vue'
import { computed } from 'vue'
import { snapshotForRoute, type ApiThemeConfig } from './api-navigation'

defineProps<{ screenMenu?: boolean }>()

const { page, theme } = useData<ApiThemeConfig>()
const prefix = computed(() => {
  const snapshots = theme.value.apiSnapshots ?? []
  return (snapshotForRoute(page.value.relativePath, snapshots) ?? snapshots.at(-1))?.prefix
})
const item = computed(() => ({
  text: 'API reference',
  link: prefix.value ? `${prefix.value}/` : '/'
}))
</script>

<template>
  <VPNavScreenMenuLink v-if="screenMenu && prefix" :item="item" />
  <VPNavBarMenuLink v-else-if="prefix" :item="item" />
</template>
