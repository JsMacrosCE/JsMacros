export type SearchResult = { id: string }

/**
 * The path prefix of the snapshot a page belongs to, or an empty string when
 * the page sits outside every snapshot. The prefix keeps whatever base the
 * site is deployed under, so it can be compared with a result id directly.
 */
export function scopePrefix(pathname: string, prefixes: string[]): string {
  for (const prefix of prefixes) {
    const at = pathname.indexOf(prefix + '/')
    if (at >= 0) return pathname.slice(0, at + prefix.length + 1)
  }
  return ''
}

/**
 * Keeps a search result when it belongs to the snapshot of the current page.
 * Results from other Minecraft versions and other mod versions are dropped,
 * so a search answers for the target the reader is actually looking at.
 *
 * VitePress keeps theme functions in the site data as source text and
 * recreates them with `Function`, so the filter cannot close over the prefix
 * list: it has to carry its own copy.
 */
export function createSearchScopeFilter(
  prefixes: string[]
): (result: SearchResult) => boolean {
  const source = `function (result) {
  var prefixes = ${JSON.stringify(prefixes)}
  var path = window.location.pathname
  for (var i = 0; i < prefixes.length; i++) {
    var at = path.indexOf(prefixes[i] + '/')
    if (at >= 0) {
      return result.id.indexOf(path.slice(0, at + prefixes[i].length + 1)) === 0
    }
  }
  return true
}`
  return new Function(`return ${source}`)() as (result: SearchResult) => boolean
}
