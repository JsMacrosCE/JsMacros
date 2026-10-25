import assert from "node:assert/strict";
import { mkdir, readdir, readFile, stat } from "node:fs/promises";
import { createServer } from "node:http";
import path from "node:path";
import { after, before, test as nodeTest } from "node:test";
import { fileURLToPath } from "node:url";
import { chromium } from "playwright-core";

// Run against the assembled site, not a substitute navigation fixture. An
// external URL also allows exercising the extracted devkit's own Python server.
const site = path.resolve(process.env.DOCS_SITE_DIR ?? fileURLToPath(new URL('./dist', import.meta.url)))
const base = process.env.DOCS_BASE_PATH ?? '/'
const navigationRequest = /\/assets\/sidebar-data\.[^/]+\.json$/
let browser, server, origin, snapshots, older, latest
const test = (name, run) => nodeTest(name, { timeout: 30000 }, run)

function entries(items) {
  return items.flatMap(item => [item, ...entries(item.items ?? [])])
}

before(async () => {
  const assets = await readdir(path.join(site, 'assets'))
  snapshots = await Promise.all(assets.filter(name => /^sidebar-data\..+\.json$/.test(name)).map(async name => {
    const data = JSON.parse(await readFile(path.join(site, 'assets', name), 'utf8'))
    return { ...data, prefix: `/${data.version}/${data.minecraftVersion}`, asset: name,
      client: entries(data.libraries).find(item => (item.text ?? item.name) === 'Client').link }
  }))
  snapshots.sort((a, b) => a.prefix.localeCompare(b.prefix, 'en', { numeric: true }))
  assert.ok(snapshots.length >= 2, 'Build at least two snapshots to exercise version switching')
  older = snapshots[0]
  latest = snapshots.at(-1)
  if (process.env.DOCS_TEST_URL) {
    origin = process.env.DOCS_TEST_URL.replace(/\/$/, '')
  } else {
    const types = { '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css',
      '.json': 'application/json', '.svg': 'image/svg+xml', '.woff2': 'font/woff2' }
    server = createServer(async (request, response) => {
      try {
        const pathname = decodeURIComponent(new URL(request.url, 'http://localhost').pathname)
        assert.ok(pathname.startsWith(base))
        let file = path.resolve(site, '.' + '/' + pathname.slice(base.length))
        assert.ok(file === site || file.startsWith(site + path.sep))
        let info = await stat(file).catch(() => null)
        if (!info?.isFile() && (await stat(file + '.html').catch(() => null))?.isFile()) file += '.html'
        else if (info?.isDirectory()) file = path.join(file, 'index.html')
        response.setHeader('Content-Type', types[path.extname(file)] ?? 'application/octet-stream')
        response.end(await readFile(file))
      } catch {
        response.writeHead(404).end('Not found')
      }
    })
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
    origin = `http://127.0.0.1:${server.address().port}`
  }
  browser = await chromium.launch({ headless: true,
    ...(process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH
      ? { executablePath: process.env.PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH } : {}) })
})

after(async () => {
  await browser?.close()
  if (server) await new Promise(resolve => server.close(resolve))
})

function href(route) { return base + route.replace(/^\//, '') }
function url(route) { return origin + href(route) }

async function pageFor(t, { expectedNetworkError = false, ...options } = {}) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, ...options })
  const page = await context.newPage()
  page.setDefaultTimeout(10000)
  const errors = []
  page.on('pageerror', error => errors.push(error.message))
  page.on('console', message => {
    if (/hydration/i.test(message.text()) || (message.type() === 'error' &&
      !(expectedNetworkError && /Failed to load resource/.test(message.text())))) errors.push(message.text())
  })
  t.after(async () => {
    await context.close()
    assert.deepEqual(errors, [], 'No browser exceptions, console errors or hydration warnings')
  })
  return page
}

async function ready(page, snapshot) {
  await page.locator(`#VPSidebarNav a[href="${href(snapshot.client)}"]`).waitFor({ state: 'attached' })
  await page.locator('.api-sidebar-status').waitFor({ state: 'hidden' })
  const links = await page.locator('#VPSidebarNav a').evaluateAll(nodes => nodes.map(node => node.getAttribute('href')))
  assert.ok(links.length > 0)
  assert.ok(links.every(link => link.startsWith(href(snapshot.prefix + '/'))), 'No links from another snapshot')
}

async function sidebarClick(page, route) {
  await page.locator(`#VPSidebarNav a[href="${href(route)}"]`).click()
  await page.waitForURL(url(route))
}

async function switchVersion(page, snapshot) {
  const menu = page.locator('.VPNavBarMenuGroup').filter({ hasText: /\d+\.\d+/ }).last()
  await menu.locator('button').click()
  await menu.locator(`a[href="${href(snapshot.prefix + '/')}"]`).click()
  await page.waitForURL(url(snapshot.prefix + '/'))
}

function gate() {
  let release
  const promise = new Promise(resolve => { release = resolve })
  return { promise, release }
}

test('server HTML and no-JS pages retain API content but no sidebar links or snapshot trees', async t => {
  const response = await fetch(url(latest.client))
  assert.equal(response.status, 200)
  const html = await response.text()
  assert.match(html, /Miscellaneous functions for interacting with Minecraft/)
  const sidebar = html.match(/<aside\b[^>]*class="[^"]*VPSidebar[\s\S]*?<\/aside>/)?.[0]
  assert.ok(sidebar, 'SSR reserves the sidebar column')
  assert.doesNotMatch(sidebar, /<a\b/)
  assert.doesNotMatch(html, /sidebar-data\.|Loading navigation|Navigation couldn’t load/)
  const page = await pageFor(t, { javaScriptEnabled: false })
  await page.goto(url(latest.client))
  assert.match(await page.locator('h1').innerText(), /^Client[\s\u200b]*$/)
  assert.equal(await page.locator('#VPSidebarNav a').count(), 0)
  await page.goto(url(latest.prefix + '/libraries'))
  const indexLinks = await page.locator('.vp-doc a').evaluateAll(nodes => nodes.map(node => new URL(node.href).pathname))
  assert.ok(indexLinks.includes(href(latest.client)), 'Server-rendered index still links to the Client API')
  await page.locator('.vp-doc a').filter({ hasText: /^Client$/ }).click()
  await page.waitForURL(url(latest.client))
  assert.match(await page.locator('h1').innerText(), /^Client[\s\u200b]*$/)
})

test('delayed navigation shows a loading state without moving desktop content', async t => {
  const page = await pageFor(t)
  const delayed = gate()
  t.after(delayed.release)
  await page.route(navigationRequest, async route => { await delayed.promise; await route.continue() })
  await page.goto(url(latest.client), { waitUntil: 'domcontentloaded' })
  await page.getByRole('status').waitFor()
  const before = await page.locator('.VPDoc .content-container').boundingBox()
  assert.equal(await page.locator('#VPSidebarNav a').count(), 0)
  delayed.release()
  await ready(page, latest)
  const after = await page.locator('.VPDoc .content-container').boundingBox()
  assert.ok(Math.abs(before.x - after.x) < 1 && Math.abs(before.width - after.width) < 1,
    `Content column moved: ${JSON.stringify({ before, after })}`)
})

test('direct member, nested-type, overview and group loads highlight the correct branch', async t => {
  const page = await pageFor(t)
  for (const snapshot of [latest, older]) {
    await page.goto(url(snapshot.client))
    await ready(page, snapshot)
    assert.equal(await page.locator('#VPSidebarNav .is-active > .item > a').getAttribute('href'), href(snapshot.client))
    const nested = entries(snapshot.classes).find(item => item.link && item.items?.some(child => child.link))
    assert.ok(nested, 'Real snapshot contains a supporting type')
    await page.goto(url(nested.items.find(child => child.link).link))
    await page.locator('#VPSidebarNav .is-active > .item > a').waitFor()
    assert.ok((await page.locator('#VPSidebarNav').innerText()).includes(nested.name))
    await page.goto(url(snapshot.prefix + '/'))
    await ready(page, snapshot)
    assert.ok(await page.locator('#VPSidebarNav a').count() > 100)
    await page.goto(url(snapshot.prefix + '/libraries.html'))
    await ready(page, snapshot)
  }
})

test('SPA navigation, history and version switching reuse successful snapshot requests', async t => {
  const page = await pageFor(t)
  const requests = []
  page.on('request', request => { if (navigationRequest.test(request.url())) requests.push(request.url()) })
  await page.goto(url(latest.client))
  await ready(page, latest)
  await page.evaluate(() => { window.__sidebarSpaMarker = true })
  await sidebarClick(page, latest.prefix + '/libraries')
  await ready(page, latest)
  await sidebarClick(page, latest.client)
  await ready(page, latest)
  await page.goBack()
  await page.waitForURL(url(latest.prefix + '/libraries'))
  await ready(page, latest)
  assert.ok(await page.locator('#VPSidebarNav a').count() > 10)
  await page.goForward()
  await page.waitForURL(url(latest.client))
  await ready(page, latest)
  await switchVersion(page, older)
  await ready(page, older)
  await switchVersion(page, latest)
  await ready(page, latest)
  assert.equal(await page.evaluate(() => window.__sidebarSpaMarker), true, 'Navigation stayed in the SPA')
  assert.equal(requests.length, 2, 'One JSON request per visited snapshot, not per page')
})

test('HTTP failure offers an index fallback and Retry recovers', async t => {
  const page = await pageFor(t, { expectedNetworkError: true })
  let requests = 0
  await page.route(navigationRequest, route => ++requests === 1
    ? route.fulfill({ status: 503, body: 'temporarily unavailable' }) : route.continue())
  await page.goto(url(latest.client))
  await page.getByRole('alert').waitFor()
  assert.equal(await page.getByRole('link', { name: 'API index', exact: true }).getAttribute('href'), href(latest.prefix + '/'))
  assert.match(await page.locator('h1').innerText(), /^Client[\s\u200b]*$/)
  await page.getByRole('button', { name: 'Retry', exact: true }).click()
  await ready(page, latest)
  assert.equal(requests, 2)
})

test('malformed JSON is evicted so Retry can recover', async t => {
  const page = await pageFor(t)
  let requests = 0
  await page.route(navigationRequest, route => ++requests === 1
    ? route.fulfill({ status: 200, contentType: 'application/json', body: '{broken' }) : route.continue())
  await page.goto(url(latest.client))
  await page.getByRole('alert').waitFor()
  await page.getByRole('button', { name: 'Retry', exact: true }).click()
  await ready(page, latest)
  assert.equal(requests, 2)
})

for (const failStale of [false, true]) {
  test(`a stale ${failStale ? 'failed' : 'successful'} response cannot replace the current snapshot`, async t => {
    const page = await pageFor(t, { expectedNetworkError: failStale })
    const delayed = gate()
    const intercepted = gate()
    const completed = gate()
    t.after(delayed.release)
    await page.route('**/' + latest.asset, async route => {
      intercepted.release()
      await delayed.promise
      if (failStale) await route.fulfill({ status: 503, body: 'stale failure' })
      else await route.continue()
      completed.release()
    })
    await page.goto(url(latest.client), { waitUntil: 'domcontentloaded' })
    await intercepted.promise
    await switchVersion(page, older)
    await ready(page, older)
    delayed.release()
    await completed.promise
    await page.waitForLoadState('networkidle')
    await ready(page, older)
    assert.equal(await page.getByRole('alert').count(), 0)
  })
}

test('changing pages during a pending snapshot request shares the request and resolves the new branch', async t => {
  const page = await pageFor(t)
  const delayed = gate()
  t.after(delayed.release)
  let requests = 0
  await page.route(navigationRequest, async route => { requests++; await delayed.promise; await route.continue() })
  await page.goto(url(latest.client), { waitUntil: 'domcontentloaded' })
  await page.getByRole('status').waitFor()
  await switchVersion(page, latest)
  delayed.release()
  await ready(page, latest)
  assert.ok(await page.locator('#VPSidebarNav a').count() > 100, 'New overview branch replaces member branch')
  assert.equal(requests, 1)
})

test('mobile menu retains Escape, backdrop, keyboard section controls and link closing', async t => {
  const page = await pageFor(t, { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })
  await page.goto(url(latest.client))
  await ready(page, latest)
  const menu = page.getByRole('button', { name: 'Menu', exact: true })
  await menu.click()
  assert.equal(await menu.getAttribute('aria-expanded'), 'true')
  await page.keyboard.press('Escape')
  await page.waitForFunction(() => document.querySelector('button.menu')?.getAttribute('aria-expanded') === 'false')
  assert.equal(await menu.evaluate(node => node === document.activeElement), true)
  await menu.click()
  await page.locator('.VPBackdrop').click({ position: { x: 380, y: 400 } })
  await page.waitForFunction(() => document.querySelector('button.menu')?.getAttribute('aria-expanded') === 'false')
  await menu.click()
  await sidebarClick(page, latest.prefix + '/classes')
  await page.waitForFunction(() => document.querySelector('button.menu')?.getAttribute('aria-expanded') === 'false')
  await menu.click()
  const section = page.locator('#VPSidebarNav .VPSidebarItem.collapsible').first()
  assert.match(await section.getAttribute('class'), /collapsed/)
  await section.locator(':scope > .item > .caret').press('Enter')
  assert.doesNotMatch(await section.getAttribute('class'), /collapsed/)
  await section.locator(':scope > .item > .caret').press('Enter')
  assert.match(await section.getAttribute('class'), /collapsed/)
  assert.ok(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'No mobile horizontal overflow')
})

test('mobile menu remains usable while loading and after a retry', async t => {
  const page = await pageFor(t, { viewport: { width: 390, height: 844 }, expectedNetworkError: true })
  const delayed = gate()
  t.after(delayed.release)
  let requests = 0
  await page.route(navigationRequest, async route => {
    if (++requests === 1) { await delayed.promise; await route.fulfill({ status: 503, body: 'retry me' }) }
    else await route.continue()
  })
  await page.goto(url(latest.client), { waitUntil: 'domcontentloaded' })
  await page.getByRole('button', { name: 'Menu', exact: true }).click()
  await page.getByRole('status').waitFor()
  delayed.release()
  await page.getByRole('button', { name: 'Retry', exact: true }).click()
  await ready(page, latest)
  assert.ok(await page.locator('.VPSidebar.open').isVisible())
})

test('non-API pages never request navigation; desktop and mobile screenshots', async t => {
  const page = await pageFor(t)
  const requests = []
  page.on('request', request => { if (navigationRequest.test(request.url())) requests.push(request.url()) })
  await page.goto(url('/'))
  await page.waitForLoadState('networkidle')
  assert.equal(requests.length, 0)
  assert.equal(await page.locator('.VPSidebar').count(), 0)
  await page.goto(url(latest.client))
  await ready(page, latest)
  if (process.env.DOCS_SCREENSHOT_DIR) {
    await mkdir(process.env.DOCS_SCREENSHOT_DIR, { recursive: true })
    await page.screenshot({ path: path.join(process.env.DOCS_SCREENSHOT_DIR, 'sidebar-desktop.png'), animations: 'disabled' })
    const mobile = await pageFor(t, { viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })
    await mobile.goto(url(latest.client))
    await ready(mobile, latest)
    await mobile.getByRole('button', { name: 'Menu', exact: true }).click()
    await mobile.locator('.VPSidebar.open').waitFor()
    await mobile.screenshot({ path: path.join(process.env.DOCS_SCREENSHOT_DIR, 'sidebar-mobile.png'), animations: 'disabled' })
  }
})
