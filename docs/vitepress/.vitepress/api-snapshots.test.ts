import assert from 'node:assert/strict'
import fs from 'node:fs'
import os from 'node:os'
import path from 'node:path'
import { test } from 'node:test'
import { loadApiSnapshots } from './api-snapshots.ts'

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
