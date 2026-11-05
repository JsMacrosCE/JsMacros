import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { test } from 'node:test'
import {
  canonicalUrl, isReferencePage, markdownTwinUrl, pagePath, showsSocialCard,
  SITE_URL, SOCIAL_IMAGE, SOCIAL_IMAGE_HEIGHT, SOCIAL_IMAGE_WIDTH
} from './seo.ts'

test('page paths match the URLs VitePress writes', () => {
  assert.equal(pagePath('index.md'), '/')
  assert.equal(pagePath('guide/index.md'), '/guide/')
  assert.equal(pagePath('guide/concepts/events.md'), '/guide/concepts/events')
  assert.equal(
    pagePath('2.0.0/1.21.8/classes/com/jsmacrosce/Pair.md'),
    '/2.0.0/1.21.8/classes/com/jsmacrosce/Pair'
  )
})

test('a canonical is absolute and drops the source extension', () => {
  assert.equal(canonicalUrl('index.md'), `${SITE_URL}/`)
  assert.equal(canonicalUrl('guide/index.md'), `${SITE_URL}/guide/`)
  assert.equal(canonicalUrl('guide/getting-started.md'), `${SITE_URL}/guide/getting-started`)
  assert.ok(!canonicalUrl('guide/index.md').includes('.md'))
})

test('only generated snapshots are reference pages', () => {
  const prefixes = ['/2.0.0/1.21.8', '/2.0.0/26.3']
  assert.equal(isReferencePage('2.0.0/1.21.8/index.md', prefixes), true)
  assert.equal(isReferencePage('2.0.0/1.21.8/classes/com/jsmacrosce/Pair.md', prefixes), true)
  assert.equal(isReferencePage('guide/index.md', prefixes), false)
  assert.equal(isReferencePage('index.md', prefixes), false)
  // A longer version must not be attributed to the shorter prefix's snapshot.
  assert.equal(isReferencePage('2.0.0/1.21.80/index.md', prefixes), false)
})

test('a page twin uses the page route, with index.md for a directory', () => {
  assert.equal(
    markdownTwinUrl('2.0.0/1.21.8/classes/com/jsmacrosce/Pair.md'),
    `${SITE_URL}/2.0.0/1.21.8/classes/com/jsmacrosce/Pair.md`
  )
  assert.equal(markdownTwinUrl('2.0.0/1.21.8/index.md'), `${SITE_URL}/2.0.0/1.21.8/index.md`)
})

test('the social image lives on the deployed origin', () => {
  assert.equal(SOCIAL_IMAGE, `${SITE_URL}/jsmacrosce-og.png`)
})

test('the social card is square, so clients use their thumbnail layout', () => {
  assert.equal(SOCIAL_IMAGE_WIDTH, SOCIAL_IMAGE_HEIGHT)
})

test('the declared social image size matches the shipped file', () => {
  // Read the PNG header rather than trusting the constants: a replaced asset must
  // not keep advertising the old dimensions.
  const file = path.join(import.meta.dirname, '../content/public/jsmacrosce-og.png')
  const header = fs.readFileSync(file).subarray(0, 24)
  assert.equal(header.toString('latin1', 12, 16), 'IHDR', 'not a PNG header')
  assert.equal(header.readUInt32BE(16), SOCIAL_IMAGE_WIDTH)
  assert.equal(header.readUInt32BE(20), SOCIAL_IMAGE_HEIGHT)
})

test('only site pages carry the card', () => {
  const prefixes = ['/2.0.0/1.21.8']
  assert.equal(showsSocialCard('index.md', prefixes), true)
  assert.equal(showsSocialCard('guide/concepts/events.md', prefixes), true)
  assert.equal(showsSocialCard('2.0.0/1.21.8/index.md', prefixes), false)
  assert.equal(showsSocialCard('2.0.0/1.21.8/classes/com/jsmacrosce/Pair.md', prefixes), false)
})
