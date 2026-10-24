import { test, afterEach, mock } from 'node:test';
import assert from 'node:assert/strict';
import { fetchShopify, fetchWooCommerce, fetchDotPe, request, main } from './refresh-catalogues.mjs';
import { mkdtemp, readFile, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const source = { slug: 'test', baseUrl: 'https://retailer.test', platform: 'SHOPIFY' };
const product = (id) => ({ id, title: `Product ${id}`, handle: String(id), variants: [] });
const json = (body, status = 200, headers = {}) => new Response(JSON.stringify(body), { status, headers });
afterEach(() => mock.restoreAll());

test('retries HTTP 5xx, 429 and timeouts on GET with bounded backoff', async () => {
  for (const failure of [() => json({}, 502), () => json({}, 429, { 'retry-after': '2' }),
    () => { throw new DOMException('timeout', 'TimeoutError'); }]) {
    let calls = 0;
    const delays = [];
    mock.method(globalThis, 'fetch', async () => ++calls < 3 ? failure() : json({ ok: true }));
    assert.deepEqual(await request(source.baseUrl, { retryNetwork: true, sleep: async (ms) => delays.push(ms) }), { ok: true });
    assert.equal(calls, 3);
    assert.equal(delays.length, 2);
    assert.ok(delays[0] >= 750);
    mock.restoreAll();
  }
});

test('does not retry imports, permanent failures, or excessive Retry-After', async () => {
  for (const [status, options, headers] of [[503, { method: 'POST' }, {}], [401, {}, {}], [429, {}, { 'retry-after': '120' }]]) {
    const fetch = mock.method(globalThis, 'fetch', async () => json({}, status, headers));
    await assert.rejects(request(source.baseUrl, { retryNetwork: true, sleep: async () => {}, ...options }));
    assert.equal(fetch.mock.callCount(), 1);
    mock.restoreAll();
  }
});

test('retries interrupted response bodies as well as initial connections', async () => {
  let calls = 0;
  mock.method(globalThis, 'fetch', async () => ++calls === 1
    ? { ok: true, json: async () => { throw new TypeError('terminated'); } } : json({ ok: true }));
  assert.deepEqual(await request(source.baseUrl, { retryNetwork: true, sleep: async () => {} }), { ok: true });
  assert.equal(calls, 2);
});

test('retains Shopify pages when a later page fails and never sends admin auth', async () => {
  mock.method(globalThis, 'fetch', async (url, options) => {
    assert.equal(options.headers.Authorization, undefined);
    return url.endsWith('page=1') ? json({ products: Array.from({ length: 250 }, (_, i) => product(i)) }) : json({}, 403);
  });
  const result = await fetchShopify(source);
  assert.equal(result.products.length, 250);
  assert.equal(result.complete, false);
  assert.match(result.issues[0], /page 2/);
});

test('malformed Shopify response is incomplete rather than an empty successful catalogue', async () => {
  mock.method(globalThis, 'fetch', async () => json({ error: 'blocked' }));
  const result = await fetchShopify(source);
  assert.equal(result.complete, false);
  assert.equal(result.products.length, 0);
});

test('incomplete WooCommerce variations preserve existing variable products', async () => {
  mock.method(globalThis, 'fetch', async (url) => {
    if (url.includes('type=variation')) return json({}, 403);
    return json([{ id: 1, type: 'variable', name: 'Sizes', permalink: 'https://retailer.test/1' },
      { id: 2, type: 'simple', name: 'Simple', permalink: 'https://retailer.test/2', prices: { price: '10000' } }]);
  });
  const result = await fetchWooCommerce(source);
  assert.equal(result.complete, false);
  assert.deepEqual(result.products.map((p) => p.externalId), ['2']);
});

test('DotPe continues after a failed route and marks stale 404s partial', async () => {
  mock.method(globalThis, 'fetch', async (url) => {
    if (url === source.baseUrl) return new Response('{"buildId":"abc"}');
    if (url.endsWith('.xml')) return new Response('<loc>https://retailer.test/product/1/one</loc><loc>https://retailer.test/product/2/two</loc>');
    if (url.includes('/1/')) return json({}, 404);
    return json({ pageProps: { product: { id: 2, name: 'Two', price: '100' } } });
  });
  const result = await fetchDotPe(source);
  assert.equal(result.complete, false);
  assert.deepEqual(result.products.map((p) => p.externalId), ['2']);
});

async function withWorkerEnv(run) {
  const dir = await mkdtemp(join(tmpdir(), 'kitradar-test-'));
  const previous = { ...process.env };
  Object.assign(process.env, { KITRADAR_API_URL: 'https://api.test', ADMIN_TOKEN: 'test-only', GITHUB_STEP_SUMMARY: join(dir, 'summary') });
  delete process.env.STORE;
  try { await run(process.env.GITHUB_STEP_SUMMARY); }
  finally {
    for (const key of Object.keys(process.env)) if (!(key in previous)) delete process.env[key];
    Object.assign(process.env, previous);
    await rm(dir, { recursive: true });
  }
}

test('one unavailable retailer warns while other stores import and summary records both', async () => {
  await withWorkerEnv(async (summary) => {
    mock.method(globalThis, 'fetch', async (url, options) => {
      if (url.startsWith('https://api.test')) assert.equal(options.headers.Authorization, 'Bearer test-only');
      else assert.equal(options.headers.Authorization, undefined);
      if (url.endsWith('/sources')) return json([source, { ...source, slug: 'down', baseUrl: 'https://down.test' }]);
      if (url.includes('/imports/raw?')) return json({ status: 'SUCCESS', upsertedCount: 1, delistedCount: 0 });
      if (url.includes('/imports/failure')) return new Response('');
      if (url.startsWith('https://down.test')) return json({}, 403);
      return json({ products: [product(1)] });
    });
    const outcomes = await main();
    assert.deepEqual(outcomes.map((row) => row.status), ['SUCCESS', 'FAILED']);
    const report = await readFile(summary, 'utf8');
    assert.match(report, /SUCCESS/);
    assert.match(report, /FAILED/);
  });
});

test('partial data uses dedicated endpoint and fails closed on older backends', async () => {
  await withWorkerEnv(async () => {
    let partialRequests = 0;
    mock.method(globalThis, 'fetch', async (url) => {
      if (url.endsWith('/sources')) return json([source]);
      if (url.includes('/imports/raw/partial?')) { partialRequests++; return json({}, 404); }
      assert.ok(!url.includes('/imports/raw?'), 'must not fall back to destructive complete import');
      return url.endsWith('page=1') ? json({ products: Array.from({ length: 250 }, (_, i) => product(i)) }) : json({}, 403);
    });
    await assert.rejects(main(), /backend import failure/);
    assert.equal(partialRequests, 1);
  });
});

test('fails when every store fails or backend reports FAILED inside HTTP 200', async () => {
  await withWorkerEnv(async () => {
    for (const backendFailure of [false, true]) {
      mock.method(globalThis, 'fetch', async (url) => {
        if (url.endsWith('/sources')) return json([source]);
        if (url.includes('/imports/failure')) return new Response('');
        if (url.includes('/imports/raw?')) return json({ status: 'FAILED' });
        return backendFailure ? json({ products: [product(1)] }) : json({}, 403);
      });
      await assert.rejects(main(), /backend import failure or no usable source data/);
      mock.restoreAll();
    }
  });
});

test('successful partial import is visible as PARTIAL and does not fail the whole run', async () => {
  await withWorkerEnv(async (summary) => {
    mock.method(globalThis, 'fetch', async (url) => {
      if (url.endsWith('/sources')) return json([source]);
      if (url.includes('/imports/raw/partial?')) return json({ status: 'PARTIAL', upsertedCount: 1, delistedCount: 0 });
      return json({ products: [product(1), { title: 'Missing ID' }] });
    });
    const outcomes = await main();
    assert.equal(outcomes[0].status, 'PARTIAL');
    assert.equal(outcomes[0].upserted, 1);
    assert.match(await readFile(summary, 'utf8'), /missing its ID or title/);
  });
});

test('hitting the Shopify pagination cap never declares the catalogue complete', async () => {
  mock.method(globalThis, 'setTimeout', (callback) => { queueMicrotask(callback); return 0; });
  mock.method(globalThis, 'fetch', async () => json({ products: Array.from({ length: 250 }, (_, i) => product(i)) }));
  const result = await fetchShopify(source);
  assert.equal(result.complete, false);
  assert.match(result.issues.at(-1), /pagination limit/);
});
