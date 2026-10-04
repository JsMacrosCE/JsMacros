import path from 'node:path'
import { defineConfig } from 'vitepress'
import { inlineHighlightPlugin } from './theme/inline-highlight'
import { createHighlighter } from 'shiki'
import { loadApiSnapshots, type SidebarEntry, type SidebarNode } from './api-snapshots'

const contentDir = path.resolve(__dirname, '../content')
const snapshots = loadApiSnapshots(contentDir)
const latest = snapshots.at(-1)
const versions = [...new Set(snapshots.map(snapshot => snapshot.version))].reverse()
const targetSelector = versions.map(version => ({
  text: `JsMacrosCE ${version}`,
  items: snapshots.filter(snapshot => snapshot.version === version).reverse().map(snapshot => ({
    text: `Minecraft ${snapshot.minecraftVersion}`,
    link: `${snapshot.prefix}/`
  }))
}))
const sidebars = Object.fromEntries(snapshots.map(snapshot => [
  `${snapshot.prefix}/`,
  (['libraries', 'classes', 'events'] as const).flatMap(group =>
    buildSidebar(snapshot.sidebar[group], `${snapshot.prefix}/${group}`, group[0].toUpperCase() + group.slice(1)))
]))

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
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      ...(latest ? [{ text: 'API reference', link: `${latest.prefix}/` }] : []),
      ...(snapshots.length ? [{ text: 'Minecraft target', items: targetSelector }] : [])
    ],
    sidebar: sidebars,
    socialLinks: [
      { icon: 'github', link: 'https://github.com/JsMacrosCE/JsMacros' }
    ],
    search: {
      provider: 'local'
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
        text: section.name,
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
