import path from 'node:path';
import { createHighlighter } from 'shiki';
import { defineConfig } from 'vitepress';
import { apiLinkPlugin } from './api-links';
import { loadApiSnapshots } from './api-snapshots';
import { createGuideSidebar } from './guide';
import { canonicalUrl, isReferencePage, markdownTwinUrl, showsSocialCard, SITE_DESCRIPTION, SITE_NAME, SITE_URL, SOCIAL_IMAGE, SOCIAL_IMAGE_HEIGHT, SOCIAL_IMAGE_WIDTH } from './seo';
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
/** Route prefixes of generated snapshots, used to tell reference pages from the guide. */
const snapshotPrefixes = snapshots.map(snapshot => snapshot.prefix)

const highlighter = await createHighlighter({
  themes: ['github-light', 'github-dark'],
  langs: ['javascript', 'typescript', 'java', 'json']
})

export default defineConfig<ApiThemeConfig>({
  lang: 'en-US',
  title: SITE_NAME,
  description: SITE_DESCRIPTION,
  srcDir: './content',
  // Plain-markdown twins are published from `content/public`, which VitePress
  // copies verbatim. They must not also become routed pages under `/public/`.
  srcExclude: ['public/**'],
  cleanUrls: true,
  // The icon and social card are served from `content/public`, the public
  // directory VitePress uses when srcDir is set. robots.txt lives there too.
  head: [
    // The SVG is the mark browsers scale; the ICO answers a bare /favicon.ico
    // request, and iOS uses the opaque PNG for a home-screen tile.
    ['link', { rel: 'icon', href: '/favicon.svg', type: 'image/svg+xml' }],
    ['link', { rel: 'icon', href: '/favicon.ico', sizes: '32x32' }],
    ['link', { rel: 'apple-touch-icon', href: '/apple-touch-icon.png' }],
    ['meta', { name: 'theme-color', content: '#ffffff' }],
    ['meta', { name: 'color-scheme', content: 'light dark' }],
    // Link-preview clients (Discord, Slack, Twitter, Mastodon) read Open Graph
    // tags and most also fall back to the Twitter card ones. `summary` is the
    // compact card; the square image that goes with it is added per page below.
    ['meta', { property: 'og:site_name', content: SITE_NAME }],
    ['meta', { property: 'og:locale', content: 'en_US' }],
    ['meta', { name: 'twitter:card', content: 'summary' }]
  ],
  // Every page gets its own address. Thousands of generated pages make an
  // exhaustive sitemap worthwhile; crawlers otherwise only find what is linked.
  sitemap: { hostname: SITE_URL },
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
  // VitePress writes the page title and description but no canonical or social
  // tags. Add them here so every generated page previews with its own text.
  transformHead({ pageData }) {
    // A not-found page must not be indexed and has no address worth claiming.
    if (pageData.isNotFound || pageData.relativePath === '404.md') {
      return [['meta', { name: 'robots', content: 'noindex' }]]
    }
    const url = canonicalUrl(pageData.relativePath)
    const description = pageData.description || SITE_DESCRIPTION
    // `ogTitle` lets a page phrase its link title differently from its <title>.
    // The home page needs it: its <title> is the site name, which would otherwise
    // print "JsMacrosCE" twice — once as og:site_name, once as og:title.
    const pageTitle = (pageData.frontmatter as any)?.ogTitle || pageData.title || SITE_NAME
    const tags = [
      ['link', { rel: 'canonical', href: url }],
      ['meta', { property: 'og:type', content: isReferencePage(pageData.relativePath, snapshotPrefixes) ? 'article' : 'website' }],
      ['meta', { property: 'og:url', content: url }],
      ['meta', { property: 'og:title', content: pageTitle }],
      ['meta', { property: 'og:description', content: description }],
      ['meta', { name: 'twitter:title', content: pageTitle }],
      ['meta', { name: 'twitter:description', content: description }]
    ]
    // Reference pages are text-only embeds: the card is the same logo on every
    // one of them, so it would only make the message taller.
    if (showsSocialCard(pageData.relativePath, snapshotPrefixes)) {
      tags.push(
        ['meta', { property: 'og:image', content: SOCIAL_IMAGE }],
        ['meta', { property: 'og:image:type', content: 'image/png' }],
        ['meta', { property: 'og:image:width', content: String(SOCIAL_IMAGE_WIDTH) }],
        ['meta', { property: 'og:image:height', content: String(SOCIAL_IMAGE_HEIGHT) }],
        ['meta', { property: 'og:image:alt', content: `${SITE_NAME} logo` }],
        ['meta', { name: 'twitter:image', content: SOCIAL_IMAGE }]
      )
    }
    // Generated reference pages have a plain-markdown twin at the same route plus
    // `.md`. The guide has no generated twin yet, so it must not advertise one.
    if (isReferencePage(pageData.relativePath, snapshotPrefixes)) {
      tags.push(['link', { rel: 'alternate', type: 'text/markdown', href: markdownTwinUrl(pageData.relativePath) }])
    }
    return tags
  },
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
