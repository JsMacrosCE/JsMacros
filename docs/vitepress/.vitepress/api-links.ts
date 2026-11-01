import type MarkdownIt from 'markdown-it'

/**
 * Guide pages are version-independent, so they cannot hard-code a Minecraft
 * target in an API link. Write `/api/…` and this rewrites it to the latest
 * generated snapshot at build time. A generated reference also contains plain
 * links inside its own snapshot, which are left untouched.
 */
export function apiLinkPlugin(md: MarkdownIt, prefix?: string): void {
  if (!prefix) return

  md.core.ruler.push('jsmacros-api-links', state => {
    for (const token of state.tokens) {
      if (token.type !== 'inline' || !token.children) continue
      for (const child of token.children) {
        if (child.type !== 'link_open') continue
        const href = child.attrGet('href')
        if (href?.startsWith('/api/')) {
          child.attrSet('href', prefix + href.slice('/api'.length))
        }
      }
    }
  })
}
