import type { MarkdownRenderer } from 'vitepress'

// Generated Javadoc examples live inside raw HTML overload panels, where
// Markdown's normal fenced-code highlighter never sees them.
const generatedExample = /<pre><code>([\s\S]*?)<\/code><\/pre>/g

function getInlineCodeLang(token: { attrs?: [string, string][] }): string | null {
  if (!token.attrs?.length) return null

  for (const [name, value] of token.attrs) {
    if (name === 'data-lang' || name === 'lang') return value || null

    if (name === 'class') {
      const match = value.match(/(?:^|\s)language-([a-z0-9_+-]+)(?:\s|$)/i)
      if (match) return match[1]
    }

    if (value === '') return name
  }

  return null
}

function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
}

function highlightInline(highlighter, code: string, lang: string): string {
  const blockHtml = highlighter.codeToHtml(code, {
    lang,
    themes: {
      light: 'github-light',
      dark: 'github-dark'
    }
  })

  const preStyle = blockHtml.match(/<pre\b[^>]*style="([^"]*)"/i)?.[1] ?? ''
  const codeHtml = blockHtml.match(/<code\b[^>]*>([\s\S]*?)<\/code>/i)?.[1]

  if (!codeHtml) {
    return `<code>${escapeHtml(code)}</code>`
  }

  return `<span class="inline-code-highlight shiki shiki-themes github-light github-dark" style="${preStyle}"><code>${codeHtml}</code></span>`
}

export function inlineHighlightPlugin(
  md: MarkdownRenderer,
  highlighter: any
) {
  const originalHtmlBlock = md.renderer.rules.html_block ??
    ((tokens, idx) => tokens[idx].content)

  md.renderer.rules.html_block = (tokens, idx, options, env, self) => {
    const html = originalHtmlBlock(tokens, idx, options, env, self)
    if (!html.includes('<pre><code>')) return html

    return html.replace(generatedExample, (_match, encoded: string) => {
      // Doclet HTML has escaped both newlines and JavaScript operators.
      // Strip any Javadoc inline tags before decoding; Shiki escapes the
      // resulting source again in its highlighted output.
      const source = md.utils.unescapeAll(
        encoded.replace(/<\/?(?:code|a)(?:\s[^>]*)?>/g, '')
      ).trim()
      return highlighter.codeToHtml(source, {
        lang: 'javascript',
        themes: { light: 'github-light', dark: 'github-dark' }
      })
    })
  }

  const originalInlineCode =
    md.renderer.rules.code_inline ??
    ((tokens, idx, options, _env, self) => self.renderToken(tokens, idx, options))

  md.renderer.rules.code_inline = (tokens, idx, options, env, self) => {
    const token = tokens[idx]
    const lang = getInlineCodeLang(token)

    if (!lang) {
      return originalInlineCode(tokens, idx, options, env, self)
    }

    return highlightInline(highlighter, token.content, lang)
  }
}
