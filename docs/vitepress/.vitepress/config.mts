import path from 'node:path'
import { defineConfig } from 'vitepress'
import { inlineHighlightPlugin } from './theme/inline-highlight'
import { createSearchScopeFilter } from './theme/search-scope'
import { createHighlighter } from 'shiki'
import { loadApiSnapshots, type SidebarEntry, type SidebarNode } from './api-snapshots'

const contentDir = path.resolve(__dirname, '../content')
const snapshots = loadApiSnapshots(contentDir)
const latest = snapshots.at(-1)
const versions = [...new Set(snapshots.map(snapshot => snapshot.version))].reverse()
const groups = ['libraries', 'classes', 'events'] as const
const targetSelector = versions.map(version => ({
  text: `JsMacrosCE ${version}`,
  items: snapshots.filter(snapshot => snapshot.version === version).reverse().map(snapshot => ({
    text: snapshot.minecraftVersion,
    link: `${snapshot.prefix}/`
  }))
}))
// The navigation is the same on every page of a snapshot. Repeating the whole
// class tree on every page makes the site as many times larger as the tree, so
// a member page shows the branch it belongs to, and the whole tree stays on
// the group pages.
const sidebars = Object.fromEntries(snapshots.flatMap(snapshot => {
  const trees = groups.map(group =>
    buildSidebar(snapshot.sidebar[group], `${snapshot.prefix}/${group}`, groupTitle(group)))
  const entries: [string, SidebarConfigItem[]][] = [[`${snapshot.prefix}/`, trees.flat()]]
  groups.forEach((group, index) => {
    const reference: SidebarConfigItem = {
      text: 'Reference',
      items: [
        { text: 'Overview', link: `${snapshot.prefix}/` },
        ...groups.map(other => ({ text: groupTitle(other), link: `${snapshot.prefix}/${other}` }))
      ]
    }
    entries.push([`${snapshot.prefix}/${group}`, [reference, ...trees[index]]])
    for (const [link, branch] of chainsOf(snapshot.sidebar[group], groupTitle(group))) {
      entries.push([link, [reference, { text: groupTitle(group), items: branch }]])
    }
  })
  return entries
}))
const searchScope = createSearchScopeFilter(snapshots.map(snapshot => snapshot.prefix))

const highlighter = await createHighlighter({
  themes: ['github-light', 'github-dark'],
  langs: ['javascript', 'typescript', 'java', 'json']
})

export default defineConfig({
  lang: 'en-US',
  title: 'JsMacrosCE',
  description: 'Minecraft mod for JavaScript/polyglot macros.',
  srcDir: './content',
  cleanUrls: true,
  // The full target matrix contains thousands of pages; VitePress defaults to 64.
  buildConcurrency: 4,
  // The site data and the page hash map are the same for every page. Keeping
  // them in the pages duplicates them thousands of times.
  metaChunk: true,
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      ...(latest ? [{ text: 'API reference', link: `${latest.prefix}/` }] : []),
      ...(latest ? [{
        component: 'MinecraftVersionSelector',
        props: { items: targetSelector, fallbackVersion: latest.minecraftVersion }
      }] : [])
    ],
    sidebar: sidebars,
    socialLinks: [
      { icon: 'github', link: 'https://github.com/JsMacrosCE/JsMacros' }
    ],
    search: {
      provider: 'local',
      options: {
        miniSearch: {
          // A search answers for the snapshot the reader is on, so results
          // from other Minecraft versions and other mod versions are dropped.
          searchOptions: { filter: searchScope }
        }
      }
    },
    outline: {
      level: [2, 3],
    },
    docFooter: {
      prev: false,
      next: false
    }
  },
  markdown: {
    // Do not retain a second representation of the large generated API pages.
    cache: false,
    config: md => {
      md.use(inlineHighlightPlugin, highlighter)
    }
  }
})

type SidebarConfigItem =
  | { text: string; link: string }
  | { text: string; link?: string; collapsed: true; items: SidebarConfigItem[] };
function isSidebarDataNode(entry: SidebarEntry): entry is SidebarNode {
  return 'name' in entry && 'items' in entry
}

function groupTitle(group: string) {
  return group[0].toUpperCase() + group.slice(1)
}

function mapSidebarEntries(entries: SidebarEntry[]): SidebarConfigItem[] {
  return entries.map((entry) => {
    if (isSidebarDataNode(entry)) {
      return {
        text: entry.name,
        link: entry.link,
        collapsed: true,
        items: mapSidebarEntries(entry.items ?? [])
      }
    }
    return {
      text: entry.text,
      link: entry.link
    }
  })
}

/**
 * The path from the group to every member page, so a page can show the
 * branch it belongs to instead of the whole tree.
 */
function chainsOf(entries: SidebarNode[], mainTitle: string): Map<string, SidebarConfigItem[]> {
  const chains = new Map<string, SidebarConfigItem[]>()
  for (const section of entries) {
    const title = section.name === 'Uncategorized' ? `Other ${mainTitle.toLowerCase()}` : section.name
    const item: SidebarConfigItem = { text: title, collapsed: true, items: [] }
    collectChains(section.items ?? [], [item], chains)
  }
  return chains
}

function collectChains(
  entries: SidebarEntry[],
  path: SidebarConfigItem[],
  chains: Map<string, SidebarConfigItem[]>
) {
  for (const entry of entries) {
    if (isSidebarDataNode(entry)) {
      const children = entry.items ?? []
      const item: SidebarConfigItem = { text: entry.name, link: entry.link, collapsed: true, items: [] }
      if (entry.link) {
        // The page of a nested type also lists the types it holds.
        chains.set(entry.link, nest([...path, { ...item, items: mapSidebarEntries(children) }]))
      }
      collectChains(children, [...path, item], chains)
    } else if (entry.link) {
      chains.set(entry.link, nest([...path, { text: entry.text, link: entry.link }]))
    }
  }
}

/** Turns a path into the nested items the sidebar renders. */
function nest(path: SidebarConfigItem[]): SidebarConfigItem[] {
  if (path.length === 0) return []
  const [head, ...rest] = path
  if (rest.length === 0) return [head]
  return [{ ...head, items: nest(rest) }]
}

function buildSidebar(entries: SidebarNode[], fallbackLink: string, mainTitle: string) {
  if (Array.isArray(entries) && entries.length > 0) {
    // Flatten the sidebar on pages like "Libraries" where we don't categorize things
    if (entries.length === 1 && entries[0].name === 'Uncategorized') {
      return [{
        text: mainTitle,
        items: mapSidebarEntries(entries[0].items ?? [])
      }];
    }

    return [{
      text: mainTitle,
      // Move Uncategorized to the end
      items: entries.sort((a, b) => {
        if (a.name === 'Uncategorized') return 1;
        if (b.name === 'Uncategorized') return -1;
        return a.name.localeCompare(b.name);
      }).map((section) => ({
        text: section.name === 'Uncategorized' ? `Other ${mainTitle.toLowerCase()}` : section.name,
        collapsed: true,
        items: mapSidebarEntries(section.items ?? [])
      }))
    }];
  }
  return [
    {
      text: mainTitle,
      items: [
        {
          text: `Browse ${mainTitle}`,
          link: fallbackLink
        }
      ]
    }
  ]
}
