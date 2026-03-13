import fs from 'node:fs'
import path from 'node:path'
import { defineConfig } from 'vitepress'
import { inlineHighlightPlugin } from './theme/inline-highlight'
import { createHighlighter } from 'shiki'

const contentDir = path.resolve(__dirname, '../content')
const versionDir = resolveVersionDir(contentDir)
const versionPrefix = `/${versionDir}`
const sidebarData = loadSidebarData(versionDir)

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
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Libraries', link: `${versionPrefix}/libraries` },
      { text: 'Classes', link: `${versionPrefix}/classes` },
      { text: 'Events', link: `${versionPrefix}/events` }
    ],
    sidebar: {
      [`${versionPrefix}/libraries`]: buildSidebar(sidebarData.libraries, `${versionPrefix}/libraries`, 'Libraries'),
      [`${versionPrefix}/classes`]: buildSidebar(sidebarData.classes, `${versionPrefix}/classes`, 'Classes'),
      [`${versionPrefix}/events`]: buildSidebar(sidebarData.events, `${versionPrefix}/events`, 'Events')
    },
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
    config: md => {
      md.use(inlineHighlightPlugin, highlighter)
    }
  }
})

function resolveVersionDir(dir: string): string {
  if (!fs.existsSync(dir)) {
    return 'latest'
  }
  const entries = fs.readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
  if (entries.length === 0) {
    return 'latest'
  }
  entries.sort((a, b) => a.localeCompare(b, undefined, { numeric: true }))
  return entries[entries.length - 1]
}

type SidebarItem = { text: string; link: string };
type SidebarDataEntry = SidebarItem | SidebarDataNode;
type SidebarDataNode = { name: string; link?: string; items: SidebarDataEntry[] };
type SidebarConfigItem =
  | { text: string; link: string }
  | { text: string; link?: string; collapsed: true; items: SidebarConfigItem[] };
type SidebarData = {
  classes: SidebarDataNode[];
  events: SidebarDataNode[];
  libraries: SidebarDataNode[];
}

function isSidebarDataNode(entry: SidebarDataEntry): entry is SidebarDataNode {
  return 'name' in entry && 'items' in entry
}

function mapSidebarEntries(entries: SidebarDataEntry[]): SidebarConfigItem[] {
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

function loadSidebarData(version: string): SidebarData {
  const dataPath = path.join(contentDir, version, 'sidebar-data.json')
  if (!fs.existsSync(dataPath)) {
    return { classes: [], events: [], libraries: [] }
  }
  try {
    return JSON.parse(fs.readFileSync(dataPath, 'utf8'))
  } catch (error) {
    console.warn('Failed to load sidebar data:', error)
    return { classes: [], events: [], libraries: [] }
  }
}

function buildSidebar(entries: SidebarDataNode[], fallbackLink: string, mainTitle: string) {
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
