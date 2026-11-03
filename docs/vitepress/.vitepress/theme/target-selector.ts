import { normalizeApiRoute } from './api-navigation.ts'

type TargetItem = { text: string; link: string }
type TargetGroup = { items: TargetItem[] }

/** Page paths omit the site's base, so selection works for subdirectory deployments too. */
export function selectedVersion(relativePath: string, groups: TargetGroup[], fallback: string): string {
  const route = `/${relativePath}`
  return groups.flatMap(group => group.items).find(item => route.startsWith(item.link))?.text ?? fallback
}

/**
 * The link to the same page on another target, or that target's overview.
 *
 * Every target publishes the same class, event and library pages under its own
 * prefix, so switching versions only has to replace that prefix. A page can be
 * absent when a target predates a type, so the caller supplies `exists` and the
 * target overview is used instead of a dead link.
 */
export function versionLink(
  relativePath: string,
  groups: TargetGroup[],
  targetLink: string,
  exists: (route: string) => boolean = () => true
): string {
  const route = normalizeApiRoute(relativePath)
  const current = currentPrefix(route, groups)
  if (!current) return targetLink

  const target = targetLink.replace(/\/$/, '')
  const suffix = route.slice(current.length)
  const link = suffix ? target + suffix : `${target}/`
  return exists(link) ? link : targetLink
}

/** Longest match, so 1.21.8 never claims a page under 1.21.80. */
function currentPrefix(route: string, groups: TargetGroup[]): string | undefined {
  return groups
    .flatMap(group => group.items)
    .map(item => item.link.replace(/\/$/, ''))
    .filter(prefix => route === prefix || route.startsWith(`${prefix}/`))
    .sort((a, b) => b.length - a.length)[0]
}

// The characters VitePress replaces in a page path when it names the chunk hash
// map entry. Class paths only use letters, digits, dots and underscores, but a
// shared helper must match VitePress's sanitizer for any future page name.
const INVALID_FILENAME_CHARS = /[\u0000-\u001F"#$&*+,:;<=>?[\]^`{|}\u007F]/g

/**
 * Whether the built site contains this page.
 *
 * VitePress writes a hash map of every page and uses it to find a route's
 * JavaScript chunk. Reading the same map keeps this check synchronous and avoids
 * fetching a second page index or guessing from sidebar data, which is loaded
 * only for the snapshot being read. Development builds have no map, so the link
 * stays optimistic and the dev server answers it.
 */
export function routeExists(route: string): boolean {
  const map = (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__
  if (!map) return true

  const path = route.replace(/^\/+/, '').replace(/\/+$/, '/index') || 'index'
  const key = path.replace(/\//g, '_').replace(INVALID_FILENAME_CHARS, '_').toLowerCase()
  return `${key}.md` in map || `${key}_index.md` in map
}
