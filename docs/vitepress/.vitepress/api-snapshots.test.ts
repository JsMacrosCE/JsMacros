import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { loadApiSnapshots } from './api-snapshots.ts'
import { routeExists, selectedVersion, versionLink } from './theme/target-selector.ts'

test('discovers isolated targets in numeric order and ignores legacy mixed docs', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'jsmacros-snapshots-'))
  try {
    fs.mkdirSync(path.join(root, '2.0.0'), { recursive: true })
    fs.writeFileSync(path.join(root, '2.0.0', 'sidebar-data.json'), '{}')
    for (const mc of ['26.1.2', '1.21.11', '1.21.8']) {
      const dir = path.join(root, '2.0.0', mc)
      fs.mkdirSync(dir)
      fs.writeFileSync(path.join(dir, 'index.md'), '# API')
      fs.writeFileSync(path.join(dir, 'sidebar-data.json'), JSON.stringify({
        version: '2.0.0', minecraftVersion: mc, libraries: [], classes: [], events: []
      }))
    }
    const snapshots = loadApiSnapshots(root)
    assert.deepEqual(snapshots.map(s => s.minecraftVersion), ['1.21.8', '1.21.11', '26.1.2'])
    assert.equal(snapshots[2].prefix, '/2.0.0/26.1.2')
    const dir = path.join(root, '2.0.0', '26.1.2')
    fs.writeFileSync(path.join(dir, 'sidebar-data.json'), JSON.stringify({
      version: '2.0.0', minecraftVersion: '1.21.8', libraries: [], classes: [], events: []
    }))
    assert.throws(() => loadApiSnapshots(root), /metadata does not match/)
  } finally {
    fs.rmSync(root, { recursive: true, force: true })
  }
})

test('an ungenerated site has no invented API target', () => {
  assert.deepEqual(loadApiSnapshots('/this-path-does-not-exist/jsmacros'), [])
})

test('selector shows the current snapshot version on overview and nested pages', () => {
  const groups = [{ items: [
    { text: '1.21.8', link: '/2.0.0/1.21.8/' },
    { text: '26.1.2', link: '/2.0.0/26.1.2/' }
  ] }, { items: [{ text: '1.21.11', link: '/1.9.0/1.21.11/' }] }]
  assert.equal(selectedVersion('2.0.0/1.21.8/index.md', groups, '26.1.2'), '1.21.8')
  assert.equal(selectedVersion('2.0.0/26.1.2/classes/Client.md', groups, '26.1.2'), '26.1.2')
  assert.equal(selectedVersion('1.9.0/1.21.11/events/Tick.md', groups, '26.1.2'), '1.21.11')
  assert.equal(selectedVersion('index.md', groups, '26.1.2'), '26.1.2')
  assert.equal(selectedVersion('2.0.0/1.21.80/index.md', groups, '26.1.2'), '26.1.2')
})

test('version links keep the same page and fall back to the target overview', () => {
  const groups = [{ items: [
    { text: '1.21.11', link: '/dev/1.21.11/' },
    { text: '26.3', link: '/dev/26.3/' }
  ] }]
  const helper = 'dev/1.21.11/classes/com/jsmacrosce/jsmacros/client/api/helper/world/entity/specialized/mob/AbstractPiglinEntityHelper.md'
  const helperRoute = '/dev/26.3/classes/com/jsmacrosce/jsmacros/client/api/helper/world/entity/specialized/mob/AbstractPiglinEntityHelper'

  assert.equal(versionLink(helper, groups, '/dev/26.3/'), helperRoute)
  assert.equal(versionLink('dev/1.21.11/index.md', groups, '/dev/26.3/'), '/dev/26.3/')
  assert.equal(versionLink('dev/1.21.11/classes.md', groups, '/dev/26.3/'), '/dev/26.3/classes')
  assert.equal(versionLink('guide/index.md', groups, '/dev/26.3/'), '/dev/26.3/')
  assert.equal(versionLink('dev/1.21.11/classes/Client.md', groups, '/dev/26.3/',
    route => route === '/dev/26.3/'), '/dev/26.3/')
})

test('version links use the longest matching snapshot prefix', () => {
  const groups = [{ items: [
    { text: '26.3', link: '/dev/26.3/' },
    { text: '26.30', link: '/dev/26.30/' }
  ] }]
  assert.equal(versionLink('dev/26.30/classes/Client.md', groups, '/dev/26.3/'),
    '/dev/26.3/classes/Client')
  assert.equal(versionLink('dev/26.3/classes/Client.md', groups, '/dev/26.30/'),
    '/dev/26.30/classes/Client')
})

test('route existence reads the built VitePress hash map', () => {
  const previous = (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__
  try {
    (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__ = {
      'dev_26.3_index.md': 'hash',
      'dev_26.3_classes.md': 'hash',
      'dev_26.3_classes_com_x_owner.inner.md': 'hash'
    }
    assert.equal(routeExists('/dev/26.3/'), true)
    assert.equal(routeExists('/dev/26.3'), true)
    assert.equal(routeExists('/dev/26.3/classes'), true)
    assert.equal(routeExists('/dev/26.3/classes/com/x/Owner.Inner'), true)
    assert.equal(routeExists('/dev/26.3/classes/Missing'), false)
  } finally {
    if (previous === undefined) {
      delete (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__
    } else {
      (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__ = previous
    }
  }
})

test('route existence trusts the dev server when no hash map is built', () => {
  const previous = (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__
  try {
    delete (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__
    assert.equal(routeExists('/dev/26.3/classes/Missing'), true)
  } finally {
    if (previous !== undefined) {
      (globalThis as { __VP_HASH_MAP__?: Record<string, string> }).__VP_HASH_MAP__ = previous
    }
  }
})
