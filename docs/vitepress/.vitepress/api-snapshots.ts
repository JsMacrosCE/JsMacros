import fs from 'node:fs'
import path from 'node:path'

export type SidebarItem = { text: string; link: string }
export type SidebarNode = { name: string; link?: string; items: SidebarEntry[] }
export type SidebarEntry = SidebarItem | SidebarNode
export type SidebarData = {
  version: string
  minecraftVersion: string
  classes: SidebarNode[]
  events: SidebarNode[]
  libraries: SidebarNode[]
}
export type ApiSnapshot = {
  version: string
  minecraftVersion: string
  prefix: string
  sidebar: SidebarData
}

/** Discover only generated, target-specific snapshots, never the old mixed-version tree. */
export function loadApiSnapshots(contentDir: string): ApiSnapshot[] {
  const snapshots: ApiSnapshot[] = []
  for (const version of directories(contentDir)) {
    for (const minecraftVersion of directories(path.join(contentDir, version))) {
      const dir = path.join(contentDir, version, minecraftVersion)
      const dataPath = path.join(dir, 'sidebar-data.json')
      if (!fs.existsSync(dataPath)) continue
      const sidebar = JSON.parse(fs.readFileSync(dataPath, 'utf8')) as SidebarData
      if (sidebar.version !== version || sidebar.minecraftVersion !== minecraftVersion) {
        throw new Error(`API snapshot metadata does not match its route: ${dir}`)
      }
      if (!fs.existsSync(path.join(dir, 'index.md'))) {
        throw new Error(`API snapshot is missing its overview: ${dir}`)
      }
      for (const group of ['libraries', 'classes', 'events'] as const) {
        if (!Array.isArray(sidebar[group])) throw new Error(`Invalid ${group} sidebar: ${dataPath}`)
      }
      snapshots.push({ version, minecraftVersion, prefix: `/${version}/${minecraftVersion}`, sidebar })
    }
  }
  return snapshots.sort((a, b) =>
    a.version.localeCompare(b.version, undefined, { numeric: true }) ||
    a.minecraftVersion.localeCompare(b.minecraftVersion, undefined, { numeric: true }))
}

function directories(dir: string): string[] {
  if (!fs.existsSync(dir)) return []
  return fs.readdirSync(dir, { withFileTypes: true }).filter(entry => entry.isDirectory()).map(entry => entry.name)
}
