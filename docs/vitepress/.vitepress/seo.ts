/**
 * Site-wide metadata for link embeds and search engines.
 *
 * Everything a crawler or chat client needs is derived from one origin so a
 * deployment to a different host only changes `SITE_URL`.
 */
export const SITE_URL = 'https://jsmacrosce.com'
export const SITE_NAME = 'JsMacrosCE'
export const SITE_DESCRIPTION = 'Minecraft mod for JavaScript/polyglot macros.'
/**
 * The social card, served from the site root by VitePress's public directory.
 *
 * It is square on purpose: Discord and Slack choose their compact thumbnail
 * layout from the image's aspect ratio, so a square card keeps a pasted link to
 * one short block instead of a full-width image below the text. A wide card
 * cannot be used with `summary`, which expects roughly 2:1.
 */
export const SOCIAL_IMAGE = `${SITE_URL}/jsmacrosce-og.png`
export const SOCIAL_IMAGE_WIDTH = 630
export const SOCIAL_IMAGE_HEIGHT = 630

/**
 * The absolute site path of a page, without a trailing `.html`.
 *
 * VitePress writes `guide/index.md` as `guide/` and `a/b.md` as `a/b`, and a
 * canonical URL has to use the same shape as the page it points at.
 */
export function pagePath(relativePath: string): string {
  const path = `/${relativePath}`
    .replace(/(^|\/)index\.md$/, '$1')
    .replace(/\.md$/, '')
  return path === '' ? '/' : path
}

/** The address a crawler should treat as the one copy of a page. */
export function canonicalUrl(relativePath: string): string {
  return `${SITE_URL}${pagePath(relativePath)}`
}

/**
 * Whether a page should carry the social card at all.
 *
 * A reference page repeats one logo across thousands of near-identical links, so
 * the image only lengthens the embed there; the title and description already say
 * which type the link points at. Site pages keep the card.
 */
export function showsSocialCard(relativePath: string, snapshotPrefixes: string[]): boolean {
  return !isReferencePage(relativePath, snapshotPrefixes)
}

/**
 * Whether a page belongs to a generated API snapshot rather than the guide.
 *
 * Snapshot pages document a single Minecraft target, so they are published as
 * articles; the guide and the home page stay `website`.
 */
export function isReferencePage(relativePath: string, snapshotPrefixes: string[]): boolean {
  const route = `/${relativePath}`
  return snapshotPrefixes.some(prefix => route.startsWith(`${prefix}/`))
}
