import assert from 'node:assert/strict'
import test from 'node:test'
import { createSearchScopeFilter, scopePrefix } from './search-scope.ts'

const prefixes = ['/2.0.0/1.21.8', '/2.0.0/26.1.2', '/2.0.0-beta.6/1.21.8']

type Filter = (result: { id: string }) => boolean

/** The filter reads the current path when it runs, so the stub has to stay. */
function atPath(pathname: string, run: (filter: Filter) => void) {
  const previous = (globalThis as any).window
  ;(globalThis as any).window = { location: { pathname } }
  try {
    run(createSearchScopeFilter(prefixes))
  } finally {
    ;(globalThis as any).window = previous
  }
}

test('a page belongs to the snapshot its path starts with', () => {
  assert.equal(scopePrefix('/2.0.0/26.1.2/classes/Client.md', prefixes), '/2.0.0/26.1.2/')
  assert.equal(scopePrefix('/2.0.0-beta.6/1.21.8/classes/Client.md', prefixes), '/2.0.0-beta.6/1.21.8/')
  assert.equal(scopePrefix('/index.md', prefixes), '')
})

test('a longer version name is not mistaken for a shorter one', () => {
  assert.equal(scopePrefix('/2.0.0-beta.6/1.21.8/classes/Client.md', prefixes), '/2.0.0-beta.6/1.21.8/')
  assert.equal(scopePrefix('/2.0.0/1.21.8/classes/Client.md', prefixes), '/2.0.0/1.21.8/')
})

test('the filter keeps only the results of the current snapshot', () => {
  atPath('/2.0.0/26.1.2/classes/Client.md', filter => {
    assert.equal(filter({ id: '/2.0.0/26.1.2/classes/Client#methods' }), true)
    assert.equal(filter({ id: '/2.0.0/26.1.2/libraries/Hud' }), true)
    assert.equal(filter({ id: '/2.0.0/1.21.8/classes/Client' }), false)
    assert.equal(filter({ id: '/2.0.0-beta.6/1.21.8/classes/Client' }), false)
  })
})

test('a page outside every snapshot searches the whole site', () => {
  atPath('/index.md', filter => {
    assert.equal(filter({ id: '/2.0.0/26.1.2/classes/Client' }), true)
    assert.equal(filter({ id: '/2.0.0/1.21.8/classes/Client' }), true)
  })
})

test('a deployed base stays part of the compared prefix', () => {
  atPath('/docs/2.0.0/26.1.2/classes/Client.md', filter => {
    assert.equal(filter({ id: '/docs/2.0.0/26.1.2/classes/Client' }), true)
    assert.equal(filter({ id: '/docs/2.0.0/1.21.8/classes/Client' }), false)
  })
})

test('the filter agrees with the prefix helper', () => {
  for (const pathname of [
    '/2.0.0/26.1.2/classes/Client.md',
    '/2.0.0/1.21.8/libraries/Hud',
    '/2.0.0-beta.6/1.21.8/events/Tick',
    '/index.md'
  ]) {
    const prefix = scopePrefix(pathname, prefixes)
    atPath(pathname, filter => {
      for (const id of [
        '/2.0.0/26.1.2/classes/Client',
        '/2.0.0/1.21.8/classes/Client',
        '/2.0.0-beta.6/1.21.8/classes/Client'
      ]) {
        assert.equal(filter({ id }), prefix === '' || id.startsWith(prefix))
      }
    })
  }
})

test('the filter survives the site data round trip', () => {
  atPath('/2.0.0/26.1.2/classes/Client.md', filter => {
    const restored = new Function(`return ${filter.toString()}`)() as Filter
    assert.equal(restored({ id: '/2.0.0/26.1.2/classes/Client' }), true)
    assert.equal(restored({ id: '/2.0.0/1.21.8/classes/Client' }), false)
  })
})
