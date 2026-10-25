import type { SidebarData } from '../api-snapshots'
import { createNavigationLoader } from './api-navigation'

// Import asset URLs, not the trees themselves. Only the active snapshot is fetched,
// and a failed fetch can be retried without a cached failed JavaScript import.
const snapshots = import.meta.glob<string>('../../content/*/*/sidebar-data.json', {
  query: '?url', import: 'default', eager: true
})

export const loadNavigation = createNavigationLoader(async snapshot => {
  const url = snapshots[`../../content${snapshot.prefix}/sidebar-data.json`]
  if (!url) throw new Error(`Missing navigation for ${snapshot.prefix}`)
  const response = await fetch(url, { cache: 'no-cache' })
  if (!response.ok) throw new Error(`Navigation request failed: HTTP ${response.status}`)
  return await response.json() as SidebarData
})
