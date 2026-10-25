import path from 'node:path';
import { createHighlighter } from 'shiki';
import { defineConfig } from 'vitepress';
import { loadApiSnapshots } from './api-snapshots';
import type { ApiThemeConfig } from './theme/api-navigation';
import { inlineHighlightPlugin } from './theme/inline-highlight';
import { createSearchScopeFilter } from './theme/search-scope';

const contentDir = path.resolve(__dirname, '../content')
const snapshots = loadApiSnapshots(contentDir)
const latest = snapshots.at(-1)
const versions = [...new Set(snapshots.map(snapshot => snapshot.version))].reverse()
const targetSelector = versions.map(version => ({
  text: `JsMacrosCE ${version}`,
  items: snapshots.filter(snapshot => snapshot.version === version).reverse().map(snapshot => ({
    text: snapshot.minecraftVersion,
    link: `${snapshot.prefix}/`
  }))
}))
const searchScope = createSearchScopeFilter(snapshots.map(snapshot => snapshot.prefix))

const highlighter = await createHighlighter({
  themes: ['github-light', 'github-dark'],
  langs: ['javascript', 'typescript', 'java', 'json']
})

export default defineConfig<ApiThemeConfig>({
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
    // Only the small target manifest belongs in global site data. The browser
    // loads one snapshot's navigation and supplies its current branch as an array.
    apiSnapshots: snapshots.map(({ version, minecraftVersion, prefix }) => ({ version, minecraftVersion, prefix })),
    sidebar: [],
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
