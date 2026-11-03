<script setup lang="ts">
import { useData } from 'vitepress'
import VPNavBarMenuGroup from 'vitepress/dist/client/theme-default/components/VPNavBarMenuGroup.vue'
import VPNavScreenMenuGroup from 'vitepress/dist/client/theme-default/components/VPNavScreenMenuGroup.vue'
import type { DefaultTheme } from 'vitepress/theme'
import { computed, onMounted, ref } from 'vue'
import { routeExists, selectedVersion, versionLink } from './target-selector'

const props = defineProps<{
  items: (DefaultTheme.NavItemChildren & { items: { text: string; link: string }[] })[]
  fallbackVersion: string
  screenMenu?: boolean
}>()
const { page } = useData()

// Server rendering and hydration both use the optimistic same-page links. The
// hash map is a browser build artifact, so checking it earlier would make the
// server and client disagree for a page the target does not publish.
const mounted = ref(false)
onMounted(() => { mounted.value = true })

const items = computed(() => props.items.map(group => ({
  ...group,
  items: group.items.map(item => ({
    ...item,
    link: versionLink(page.value.relativePath, props.items, item.link, mounted.value ? routeExists : undefined)
  }))
})))
const text = computed(() => selectedVersion(page.value.relativePath, props.items, props.fallbackVersion))
const item = computed(() => ({ text: text.value, items: items.value }))
</script>

<template>
  <VPNavScreenMenuGroup v-if="screenMenu" :text="text" :items="items" />
  <VPNavBarMenuGroup v-else :item="item" />
</template>
