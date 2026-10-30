import path from 'node:path';
import { createHighlighter } from 'shiki';
import { defineConfig } from 'vitepress';
import { apiLinkPlugin } from './api-links';
import { loadApiSnapshots } from './api-snapshots';
import { createGuideSidebar } from './guide';
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
const searchScope = createSearchScopeFilter(
  snapshots.map(snapshot => snapshot.prefix),
  '/guide/',
  latest?.prefix
)

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
  // The home-page hero cannot read the generated snapshots from frontmatter,
  // so point its API action at the newest target during the build.
  transformPageData(pageData) {
    if (!latest || pageData.relativePath !== 'index.md') return
    const hero = (pageData.frontmatter as any)?.hero
    if (!Array.isArray(hero?.actions)) return
    for (const action of hero.actions) {
      if (action?.text === 'API reference') action.link = `${latest.prefix}/`
    }
  },
  // The full target matrix contains thousands of pages; VitePress defaults to 64.
  buildConcurrency: 4,
  // The site data and the page hash map are the same for every page. Keeping
  // them in the pages duplicates them thousands of times.
  metaChunk: true,
  themeConfig: {
    nav: [
      { text: 'Home', link: '/' },
      { text: 'Guide', link: '/guide/' },
      ...(latest ? [{ component: 'ApiReferenceLink' }] : []),
      ...(latest ? [{
        component: 'MinecraftVersionSelector',
        props: { items: targetSelector, fallbackVersion: latest.minecraftVersion }
      }] : [])
    ],
    // Only the small target manifest belongs in global site data. The browser
    // loads one snapshot's navigation and supplies its current branch as an array.
    apiSnapshots: snapshots.map(({ version, minecraftVersion, prefix }) => ({ version, minecraftVersion, prefix })),
    // The guide is version-independent. API routes replace this with the
    // navigation loaded for their snapshot in BrowserSidebarLayout.
    sidebar: { '/guide/': createGuideSidebar(latest?.prefix) },
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
      md.use(apiLinkPlugin, latest?.prefix)
    }
  }
})
