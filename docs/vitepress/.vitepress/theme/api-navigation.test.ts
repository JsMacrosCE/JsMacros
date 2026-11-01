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
const other = summary.prefix + '/classes/Other'
const types = [{ text: 'EntityHelpers', link: summary.prefix + '/types/EntityHelpers' }]

function fixture(): SidebarData {
  return {
    ...summary,
    libraries: [{ name: 'Uncategorized', items: [{
      name: 'Client', link: client, items: [{
        name: 'Result', link: result, items: [{ text: 'Value', link: value }]
      }]
    }] }],
    classes: [
      { name: 'Uncategorized', items: [{ text: 'Other', link: other }] },
      { name: 'World', items: [{ name: 'Entities', items: [
        { text: 'EntityHelper', link: entity }, { text: 'EntityHelperExtra', link: otherEntity }
      ] }] },
      { name: 'Rendering', items: [] }
    ],
    events: [],
    types
  }
}

function group(items: any[], text: string): any {
  const found = items.find(item => item.text === text)
  assert.ok(found, `Missing sidebar group ${text}`)
  return found
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

test('every page keeps the same group headers and the full tree', () => {
  const nav = createSnapshotNavigation(fixture(), summary)
  const page = nav.sidebar(`${entity}.md`)
  assert.deepEqual(page.map(item => item.text), ['Overview', 'Libraries', 'Classes', 'Events', 'Type namespaces'])
  assert.equal(page[0].link, `${summary.prefix}/`)
  assert.equal(group(page, 'Libraries').collapsed, true)
  assert.equal(group(page, 'Classes').collapsed, false, 'the active group stays open')
  assert.equal(group(page, 'Events').collapsed, true)
  assert.equal(group(page, 'Type namespaces').collapsed, true)
  assert.deepEqual(nav.sidebar(`${summary.prefix}/`).map(item => item.collapsed ?? false), [false, true, true, true, true])
  assert.equal(group(nav.sidebar(`${summary.prefix}/classes`), 'Classes').collapsed, false)
  assert.equal(group(nav.sidebar(`${result}.md`), 'Libraries').collapsed, false)
  assert.equal(group(nav.sidebar(`${result}.md`), 'Classes').collapsed, true)
})

test('group and member pages expose the full tree, not only the active branch', () => {
  const nav = createSnapshotNavigation(fixture(), summary)
  const page = nav.sidebar(`${entity}.md`)

  const libraries = group(page, 'Libraries')
  assert.equal(libraries.link, `${summary.prefix}/libraries`)
  assert.deepEqual(libraries.items, [{
    text: 'Client', link: client, collapsed: true, items: [{
      text: 'Result', link: result, collapsed: true, items: [{ text: 'Value', link: value }]
    }]
  }])

  const classes = group(page, 'Classes')
  assert.deepEqual(classes.items!.map(item => item.text), ['World', 'Other classes'])
  assert.ok(JSON.stringify(classes).includes(otherEntity))
  assert.ok(JSON.stringify(classes).includes(other))
  assert.deepEqual(group(page, 'Type namespaces').items, types)
})

test('empty groups keep their header but have no children', () => {
  const nav = createSnapshotNavigation(fixture(), summary)
  const events = group(nav.sidebar(`${summary.prefix}/events`), 'Events')
  assert.equal(events.link, `${summary.prefix}/events`)
  assert.deepEqual(events.items, [])
})

test('routes outside the snapshot have no sidebar and metadata mismatches are rejected', () => {
  const data = fixture()
  const before = structuredClone(data)
  assert.deepEqual(createSnapshotNavigation(data, summary).sidebar('/2.0.0/1.21.5/classes/example/EntityHelper'), [])
  assert.throws(() => createSnapshotNavigation({ ...data, minecraftVersion: '1.21.5' }, summary), /metadata does not match/)
  assert.throws(() => createSnapshotNavigation({ ...data, classes: null } as unknown as SidebarData, summary), /Invalid classes/)
  assert.throws(() => createSnapshotNavigation({ ...data, types: null } as unknown as SidebarData, summary), /Invalid types/)
  assert.deepEqual(data, before)
})

test('snapshots without type namespaces still load', () => {
  const data = fixture()
  delete data.types
  const nav = createSnapshotNavigation(data, summary)
  assert.deepEqual(nav.sidebar(`${summary.prefix}/`).map(item => item.text),
    ['Overview', 'Libraries', 'Classes', 'Events'])
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
