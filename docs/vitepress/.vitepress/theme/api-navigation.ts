import type { DefaultTheme } from 'vitepress/theme'
import type { SidebarData, SidebarEntry, SidebarNode } from '../api-snapshots'

export type SnapshotSummary = { version: string; minecraftVersion: string; prefix: string }
export type ApiThemeConfig = DefaultTheme.Config & { apiSnapshots: SnapshotSummary[] }
type Group = 'libraries' | 'classes' | 'events'
type Item = DefaultTheme.SidebarItem
type Member = { group: Group; path: Item[]; children?: SidebarEntry[] }
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

/** Index one immutable snapshot, not a global table containing every page's expanded branch. */
export function createSnapshotNavigation(data: SidebarData, expected: SnapshotSummary): SnapshotNavigation {
  if (data.version !== expected.version || data.minecraftVersion !== expected.minecraftVersion) {
    throw new Error(`Navigation metadata does not match ${expected.prefix}`)
  }
  for (const group of groups) {
    if (!Array.isArray(data[group])) throw new Error(`Invalid ${group} navigation in ${expected.prefix}`)
  }
  const reference: Item = {
    text: 'Reference',
    items: [
      { text: 'Overview', link: `${expected.prefix}/` },
      ...groups.map(group => ({ text: title(group), link: `${expected.prefix}/${group}` }))
    ]
  }
  const members = new Map<string, Member>()
  for (const group of groups) {
    for (const section of data[group]) {
      const sectionTitle = section.name === 'Uncategorized' ? `Other ${group}` : section.name
      visit(section.items, [{ text: sectionTitle, collapsed: true }], group)
    }
  }
  function visit(entries: SidebarEntry[], parents: Item[], group: Group) {
    for (const entry of entries) {
      const item: Item = isNode(entry)
        ? { text: entry.name, link: entry.link, collapsed: true }
        : { text: entry.text, link: entry.link }
      const path = [...parents, item]
      if (entry.link) members.set(normalizeApiRoute(entry.link), {
        group, path, ...(isNode(entry) ? { children: entry.items } : {})
      })
      if (isNode(entry)) visit(entry.items, path, group)
    }
  }
  const trees = new Map<Group, Item[]>()
  function tree(group: Group): Item[] {
    if (!trees.has(group)) trees.set(group, groupTree(data[group], group, expected.prefix))
    return trees.get(group)!
  }
  return {
    prefix: expected.prefix,
    sidebar(relativePath) {
      const route = normalizeApiRoute(relativePath)
      if (route === expected.prefix) return groups.flatMap(tree)
      for (const group of groups) {
        if (route === `${expected.prefix}/${group}`) return [reference, ...tree(group)]
      }
      const member = members.get(route)
      if (!member) return []
      const path = member.path.map(item => ({ ...item }))
      if (member.children) path[path.length - 1].items = mapEntries(member.children)
      for (let index = path.length - 2; index >= 0; index--) path[index].items = [path[index + 1]]
      return [reference, { text: title(member.group), items: [path[0]] }]
    }
  }
}

function isNode(entry: SidebarEntry): entry is SidebarNode {
  return 'name' in entry && 'items' in entry
}

function title(group: Group): string {
  return group[0].toUpperCase() + group.slice(1)
}

function mapEntries(entries: SidebarEntry[]): Item[] {
  return entries.map(entry => isNode(entry)
    ? { text: entry.name, link: entry.link, collapsed: true, items: mapEntries(entry.items) }
    : { text: entry.text, link: entry.link })
}

function groupTree(entries: SidebarNode[], group: Group, prefix: string): Item[] {
  if (!entries.length) return [{ text: title(group), items: [{ text: `Browse ${title(group)}`, link: prefix + '/' + group }] }]
  if (entries.length === 1 && entries[0].name === 'Uncategorized') {
    return [{ text: title(group), items: mapEntries(entries[0].items) }]
  }
  return [{
    text: title(group),
    items: [...entries].sort((a, b) => {
      if (a.name === 'Uncategorized') return 1
      if (b.name === 'Uncategorized') return -1
      return a.name.localeCompare(b.name)
    }).map(section => ({
      text: section.name === 'Uncategorized' ? `Other ${group}` : section.name,
      collapsed: true,
      items: mapEntries(section.items)
    }))
  }]
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
