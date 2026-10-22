import assert from 'node:assert/strict'
import test from 'node:test'
import { inlineHighlightPlugin } from './inline-highlight.ts'

function renderer() {
  const calls: { code: string; lang: string }[] = []
  const md: any = {
    renderer: { rules: {} },
    utils: {
      unescapeAll: (text: string) => text.replace(/&#10;/g, '\n')
        .replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&')
    }
  }
  const highlighter = {
    codeToHtml(code: string, options: { lang: string }) {
      calls.push({ code, lang: options.lang })
      return `<pre data-language="${options.lang}"><code>highlighted</code></pre>`
    }
  }
  inlineHighlightPlugin(md, highlighter)
  const render = (html: string) => md.renderer.rules.html_block([{ content: html }], 0)
  return { calls, render }
}

test('unmarked generated examples remain JavaScript', () => {
  const { calls, render } = renderer()
  assert.match(render('<pre><code>const draw = Hud.createDraw2D();</code></pre>'), /data-language="javascript"/)
  assert.deepEqual(calls, [{ code: 'const draw = Hud.createDraw2D();', lang: 'javascript' }])
})

test('TypeScript generated examples retain their language and operators', () => {
  const { calls, render } = renderer()
  const html = '<pre><code class="language-typescript">&#10;const value: number = 1;&#10;if (value &lt; 2 &amp;&amp; ready) run();&#10;</code></pre>'
  assert.match(render(html), /data-language="typescript"/)
  assert.deepEqual(calls, [{
    code: 'const value: number = 1;\nif (value < 2 && ready) run();', lang: 'typescript'
  }])
})

test('mixed example languages in one overload group are independent', () => {
  const { calls, render } = renderer()
  render('<div><pre><code>run();</code></pre><pre><code class="language-typescript">run(value as any);</code></pre></div>')
  assert.deepEqual(calls.map(call => call.lang), ['javascript', 'typescript'])
})

test('Javadoc inline tags are removed before highlighting', () => {
  const { calls, render } = renderer()
  render('<pre><code class="language-typescript">const value: <code>BlockHelper</code> = <a href="block">block</a>;</code></pre>')
  assert.deepEqual(calls, [{ code: 'const value: BlockHelper = block;', lang: 'typescript' }])
})

test('unrelated HTML is left untouched', () => {
  const { calls, render } = renderer()
  const html = '<div><code>plain inline code</code></div>'
  assert.equal(render(html), html)
  assert.deepEqual(calls, [])
})
