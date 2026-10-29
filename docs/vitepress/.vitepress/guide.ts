import type { DefaultTheme } from 'vitepress/theme'

/**
 * The guide is version-independent, so its sidebar is static.
 * API pages replace this with the snapshot navigation loaded for their
 * Minecraft target.
 */
export function createGuideSidebar(apiPrefix?: string): DefaultTheme.SidebarItem[] {
  const reference: DefaultTheme.SidebarItem[] = [
    { text: 'How to read the API reference', link: '/guide/reference/how-to-read' }
  ]
  if (apiPrefix) reference.push({ text: 'Open the API reference', link: `${apiPrefix}/` })

  return [
    {
      text: 'Guide',
      items: [
        { text: 'Overview', link: '/guide/' },
        { text: 'Install and first script', link: '/guide/getting-started' }
      ]
    },
    {
      text: 'Concepts',
      items: [
        { text: 'How a script runs', link: '/guide/concepts/how-scripts-run' },
        { text: 'Profiles', link: '/guide/concepts/profiles' },
        { text: 'Threading and the main thread', link: '/guide/concepts/threading' },
        { text: 'Callbacks', link: '/guide/concepts/callbacks' },
        { text: 'Events', link: '/guide/concepts/events' },
        { text: 'Sharing state', link: '/guide/concepts/shared-state' }
      ]
    },
    {
      text: 'Working with the game',
      items: [
        { text: 'Player and world', link: '/guide/game/player-and-world' },
        { text: 'Key binds and input', link: '/guide/game/keybinds' },
        { text: 'HTTP and WebSockets', link: '/guide/game/requests' }
      ]
    },
    {
      text: 'On screen',
      items: [
        { text: 'HUD and Draw2D', link: '/guide/ui/hud-and-draw2d' },
        { text: 'Custom screens', link: '/guide/ui/screens' }
      ]
    },
    {
      text: 'Advanced',
      items: [
        { text: 'Finding blocks with WorldScanner', link: '/guide/advanced/world-scanner' },
        { text: 'Script type namespaces', link: '/guide/advanced/script-types' },
        { text: 'Raw Minecraft access and reflection', link: '/guide/advanced/raw-minecraft' }
      ]
    },
    {
      text: 'Reference',
      items: reference
    }
  ]
}
