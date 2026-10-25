<script setup lang="ts">
import DefaultTheme from 'vitepress/theme'
import { useData, withBase } from 'vitepress'
import { dataSymbol } from 'vitepress/dist/client/app/data'
import { computed, markRaw, onMounted, provide, ref, shallowRef, watch } from 'vue'
import { snapshotForRoute, type ApiThemeConfig, type SnapshotNavigation } from './api-navigation'
import { loadNavigation } from './sidebar-loader'

const data = useData<ApiThemeConfig>()
const navigation = shallowRef<SnapshotNavigation>()
const failed = ref(false)
const retry = ref(0)
const snapshot = computed(() => data.page.value.isNotFound ? undefined :
  snapshotForRoute(data.page.value.relativePath, data.theme.value.apiSnapshots ?? []))
const ready = computed(() => snapshot.value && navigation.value?.prefix === snapshot.value.prefix)

// Reserve the normal responsive sidebar column without rendering its links or
// loading its data during SSR. This also keeps initial hydration/layout stable.
const placeholder = markRaw([{ items: [] }])
const sidebar = computed(() => {
  if (!snapshot.value) return []
  return ready.value
    ? markRaw(navigation.value!.sidebar(data.page.value.relativePath))
    : placeholder
})

// The built-in theme reads readonly site data. Override only this layout's theme
// injection, never the singleton shared between server-rendered pages.
provide(dataSymbol, {
  ...data,
  theme: computed(() => ({ ...data.theme.value, sidebar: sidebar.value }))
})

if (!import.meta.env.SSR) {
  onMounted(() => {
    watch([() => data.page.value.relativePath, retry], async (_value, _old, onCleanup) => {
      let active = true
      onCleanup(() => { active = false })
      failed.value = false
      const target = snapshot.value
      if (!target || navigation.value?.prefix === target.prefix) return
      try {
        const result = await loadNavigation(target)
        if (active) navigation.value = markRaw(result)
      } catch {
        if (active) failed.value = true
      }
    }, { immediate: true })
  })
}
</script>

<template>
  <DefaultTheme.Layout>
    <template #sidebar-nav-before>
      <ClientOnly>
        <div v-if="snapshot && !ready" class="api-sidebar-status" :role="failed ? 'alert' : 'status'">
          <template v-if="failed">
            <p>Navigation couldn’t load.</p>
            <button type="button" @click="retry++">Retry</button>
            <a :href="withBase(snapshot.prefix + '/')">API index</a>
          </template>
          <p v-else>Loading navigation…</p>
        </div>
      </ClientOnly>
    </template>
  </DefaultTheme.Layout>
</template>

<style scoped>
.api-sidebar-status {
  padding: 12px 0;
  color: var(--vp-c-text-2);
  font-size: 14px;
}

.api-sidebar-status button,
.api-sidebar-status a {
  margin-right: 16px;
  color: var(--vp-c-brand-1);
}

.api-sidebar-status button:hover,
.api-sidebar-status a:hover {
  text-decoration: underline;
}

.api-sidebar-status button:focus-visible,
.api-sidebar-status a:focus-visible {
  outline: 2px solid var(--vp-c-brand-1);
  outline-offset: 4px;
}
</style>
