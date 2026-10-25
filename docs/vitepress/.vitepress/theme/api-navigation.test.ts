import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createNavigationLoader, createSnapshotNavigation, normalizeApiRoute, snapshotForRoute } from './api-navigation.ts'
import type { SidebarData } from '../api-snapshots.ts'

const summary = { version: '2.0.0', minecraftVersion: '26.3', prefix: '/2.0.0/26.3' }
const client = summary.prefix + '/libraries/example/Client'
const result = client + '.Result'
const value = result + '.Value'
const entity = summary.prefix + '/classes/example/EntityHelper'
const otherEntity = summary.prefix + '/classes/example/EntityHelperExtra'

function fixture(): SidebarData {
  return {
    ...summary,
    libraries: [{ name: 'Uncategorized', items: [{
      name: 'Client', link: client, items: [{
        name: 'Result', link: result, items: [{ text: 'Value', link: value }]
      }]
    }] }],
    classes: [
      { name: 'Uncategorized', items: [{ text: 'Other', link: summary.prefix + '/classes/Other' }] },
      { name: 'World', items: [{ name: 'Entities', items: [
        { text: 'EntityHelper', link: entity }, { text: 'EntityHelperExtra', link: otherEntity }
      ] }] },
      { name: 'Rendering', items: [] }
    ],
    events: []
  }
}

test('snapshot selection uses exact version boundaries and base-free page paths', () => {
  const snapshots = [summary, { version: '2.0.0', minecraftVersion: '26.30', prefix: '/2.0.0/26.30' }]
  assert.equal(snapshotForRoute('2.0.0/26.3/index.md', snapshots), summary)
  assert.equal(snapshotForRoute('2.0.0/26.3/classes/example/EntityHelper.md', snapshots), summary)
  assert.equal(snapshotForRoute('/2.0.0/26.30/', snapshots), snapshots[1])
  assert.equal(snapshotForRoute('2.0.0/26.31/classes/Other.md', snapshots), undefined)
  assert.equal(snapshotForRoute('index.md', snapshots), undefined)
})

test('normalizes Markdown, clean URLs, HTML and index routes without losing dotted class names', () => {
  for (const path of [client, client + '.md', client + '.html', client + '?q=value#test']) {
    assert.equal(normalizeApiRoute(path), client)
  }
  assert.equal(normalizeApiRoute(`${result}.md`), result)
  assert.equal(normalizeApiRoute(`${summary.prefix}/index.md`), summary.prefix)
  assert.equal(normalizeApiRoute(`${summary.prefix}/index.html`), summary.prefix)
  assert.equal(normalizeApiRoute(`${summary.prefix}/`), summary.prefix)
})

test('member pages keep only their owner chain and preserve cross-group references', () => {
  const nav = createSnapshotNavigation(fixture(), summary)
  assert.deepEqual(nav.sidebar(`${entity}.md`), [
    { text: 'Reference', items: [
      { text: 'Overview', link: `${summary.prefix}/` },
      { text: 'Libraries', link: `${summary.prefix}/libraries` },
      { text: 'Classes', link: `${summary.prefix}/classes` },
      { text: 'Events', link: `${summary.prefix}/events` }
    ] },
    { text: 'Classes', items: [{ text: 'World', collapsed: true, items: [
      { text: 'Entities', link: undefined, collapsed: true, items: [{ text: 'EntityHelper', link: entity }] }
    ] }] }
  ])
  const extra = JSON.stringify(nav.sidebar(otherEntity))
  assert.ok(extra.includes('EntityHelperExtra'))
  assert.ok(!extra.includes(`"link":"${entity}"`))
  assert.deepEqual(nav.sidebar(`${entity}Unknown`), [])
})

test('nested types retain their own children and no unrelated siblings', () => {
  const nav = createSnapshotNavigation(fixture(), summary)
  const branch = nav.sidebar(result)
  assert.equal(branch[1].items![0].text, 'Other libraries')
  const owner = branch[1].items![0].items![0]
  assert.equal(owner.link, client)
  assert.equal(owner.items!.length, 1)
  assert.equal(owner.items![0].link, result)
  assert.deepEqual(owner.items![0].items, [{ text: 'Value', link: value }])
  assert.ok(JSON.stringify(nav.sidebar(value)).includes(value))
})

test('overview and group pages retain full trees, numeric categories and immutable input', () => {
  const data = fixture()
  const before = structuredClone(data)
  const nav = createSnapshotNavigation(data, summary)
  assert.deepEqual(nav.sidebar(`${summary.prefix}/index.md`).map(item => item.text), ['Libraries', 'Classes', 'Events'])
  const classes = nav.sidebar(`${summary.prefix}/classes.md`)
  assert.deepEqual(classes[1].items!.map(item => item.text), ['Rendering', 'World', 'Other classes'])
  assert.ok(JSON.stringify(classes).includes(otherEntity))
  assert.equal(nav.sidebar(`${summary.prefix}/libraries`)[1].items![0].text, 'Client')
  assert.deepEqual(nav.sidebar(`${summary.prefix}/events`)[1].items, [{ text: 'Browse Events', link: summary.prefix + '/events' }])
  assert.deepEqual(data, before)
})

test('different snapshots cannot leak routes or accept mismatched metadata', () => {
  const data = fixture()
  assert.throws(() => createSnapshotNavigation({ ...data, minecraftVersion: '1.21.5' }, summary), /metadata does not match/)
  assert.throws(() => createSnapshotNavigation({ ...data, classes: null } as unknown as SidebarData, summary), /Invalid classes/)
  assert.deepEqual(createSnapshotNavigation(data, summary).sidebar('/2.0.0/1.21.5/classes/example/EntityHelper'), [])
})

test('concurrent navigation shares one immutable snapshot load', async () => {
  let calls = 0
  const load = createNavigationLoader(async () => { calls++; return fixture() })
  const [first, second] = await Promise.all([load(summary), load(summary)])
  assert.equal(calls, 1)
  assert.equal(first, second)
  assert.equal(await load(summary), first)
})

test('failed loads and invalid metadata are evicted for a later retry', async () => {
  let calls = 0
  const load = createNavigationLoader(async () => {
    calls++
    if (calls === 1) throw new Error('temporary failure')
    if (calls === 2) return { ...fixture(), minecraftVersion: 'wrong' }
    return fixture()
  })
  await assert.rejects(load(summary), /temporary failure/)
  await assert.rejects(load(summary), /metadata does not match/)
  assert.equal((await load(summary)).prefix, summary.prefix)
  assert.equal(calls, 3)
})
