type TargetGroup = { items: { text: string; link: string }[] }

/** Page paths omit the site's base, so selection works for subdirectory deployments too. */
export function selectedVersion(relativePath: string, groups: TargetGroup[], fallback: string): string {
  const route = `/${relativePath}`
  return groups.flatMap(group => group.items).find(item => route.startsWith(item.link))?.text ?? fallback
}
