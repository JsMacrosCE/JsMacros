import type { DefaultTheme } from 'vitepress/theme'
import type { SidebarData, SidebarEntry, SidebarNode } from '../api-snapshots'

export type SnapshotSummary = { version: string; minecraftVersion: string; prefix: string }
export type ApiThemeConfig = DefaultTheme.Config & { apiSnapshots: SnapshotSummary[] }
type Group = 'libraries' | 'classes' | 'events'
type Item = DefaultTheme.SidebarItem
type Mapped = { item: Item; active: boolean }
const groups: Group[] = ['libraries', 'classes', 'events']

export type SnapshotNavigation = {
  prefix: string
  sidebar: (relativePath: string) => Item[]
}

export function normalizeApiRoute(relativePath: string): string {
  const route = `/${relativePath.replace(/^\/+/, '').split(/[?#]/, 1)[0]}`
  return route.replace(/\/index\.(?:md|html)$/, '/').replace(/\.(?:md|html)$/, '').replace(/\/+$/, '') || '/'
}

export function snapshotForRoute(relativePath: string, snapshots: SnapshotSummary[]): SnapshotSummary | undefined {
  const route = normalizeApiRoute(relativePath)
  return snapshots.find(snapshot => route === snapshot.prefix || route.startsWith(`${snapshot.prefix}/`))
}

/**
 * Index one immutable snapshot, not a global table containing every page's
 * expanded branch.
 *
 * Every group keeps its full tree. Only the group, category and owner chain that
 * contain the current page is expanded; everything else starts collapsed. The
 * flags are recomputed for each route because VitePress keeps a section's open
 * state on the sidebar item: without an explicit reset, a group opened on one
 * page would stay open after an in-page navigation to another group.
 */
export function createSnapshotNavigation(data: SidebarData, expected: SnapshotSummary): SnapshotNavigation {
  if (data.version !== expected.version || data.minecraftVersion !== expected.minecraftVersion) {
    throw new Error(`Navigation metadata does not match ${expected.prefix}`)
  }
  for (const group of groups) {
    if (!Array.isArray(data[group])) throw new Error(`Invalid ${group} navigation in ${expected.prefix}`)
  }
  if (data.types !== undefined && !Array.isArray(data.types)) {
    throw new Error(`Invalid types navigation in ${expected.prefix}`)
  }

  return {
    prefix: expected.prefix,
    sidebar(relativePath) {
      const route = normalizeApiRoute(relativePath)
      if (route !== expected.prefix && !route.startsWith(`${expected.prefix}/`)) return []

      const sidebar: Item[] = [{ text: 'Overview', link: `${expected.prefix}/` }]
      for (const group of groups) {
        sidebar.push(groupItem(title(group), `${expected.prefix}/${group}`,
          categoryItems(data[group], group, route), route))
      }
      if ((data.types ?? []).length) {
        sidebar.push(groupItem('Type namespaces', `${expected.prefix}/types`,
          mapEntries(data.types ?? [], route), route))
      }
      return sidebar
    }
  }
}

function isNode(entry: SidebarEntry): entry is SidebarNode {
  return 'name' in entry && 'items' in entry
}

function title(group: Group): string {
  return group[0].toUpperCase() + group.slice(1)
}

function groupItem(text: string, link: string, children: Mapped[], route: string): Item {
  const active = isUnder(link, route) || children.some(child => child.active)
  return {
    text,
    link,
    collapsed: !active,
    items: children.map(child => child.item)
  }
}

/** Category nodes are collapsed unless a page in them is open. */
function categoryItems(entries: SidebarNode[], group: Group, route: string): Mapped[] {
  const visible = entries.filter(section => section.items.length)
  if (!visible.length) return []
  if (visible.length === 1 && visible[0].name === 'Uncategorized') {
    return mapEntries(visible[0].items, route)
  }
  return [...visible].sort((a, b) => {
    if (a.name === 'Uncategorized') return 1
    if (b.name === 'Uncategorized') return -1
    return a.name.localeCompare(b.name)
  }).map(section => {
    const children = mapEntries(section.items, route)
    return {
      item: {
        text: section.name === 'Uncategorized' ? `Other ${group}` : section.name,
        collapsed: !children.some(child => child.active),
        items: children.map(child => child.item)
      },
      active: children.some(child => child.active)
    }
  })
}

function mapEntries(entries: SidebarEntry[], route: string): Mapped[] {
  return entries.map(entry => {
    if (!isNode(entry)) {
      return { item: { text: entry.text, link: entry.link }, active: isUnder(entry.link, route) }
    }
    const children = mapEntries(entry.items, route)
    const active = isUnder(entry.link, route) || children.some(child => child.active)
    return {
      item: {
        text: entry.name,
        link: entry.link,
        collapsed: !active,
        items: children.map(child => child.item)
      },
      active
    }
  })
}

/** Group links use `/` before a page; nested type links add `.` to their owner. */
function isUnder(link: string | undefined, route: string): boolean {
  if (!link) return false
  return route === link || route.startsWith(`${link}/`) || route.startsWith(`${link}.`)
}

/** Failed loads are evicted so the sidebar's retry action can recover. */
export function createNavigationLoader(load: (snapshot: SnapshotSummary) => Promise<SidebarData>) {
  const pending = new Map<string, Promise<SnapshotNavigation>>()
  return (snapshot: SnapshotSummary): Promise<SnapshotNavigation> => {
    if (!pending.has(snapshot.prefix)) {
      const result = Promise.resolve().then(() => load(snapshot))
        .then(data => createSnapshotNavigation(data, snapshot))
        .catch(error => { pending.delete(snapshot.prefix); throw error })
      pending.set(snapshot.prefix, result)
    }
    return pending.get(snapshot.prefix)!
  }
}
