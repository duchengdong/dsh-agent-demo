import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import test from 'node:test';

import { createRequestHandler } from '../src/app.js';

async function startServer(t) {
  const server = createServer(createRequestHandler());
  await new Promise((resolve) => server.listen(0, '127.0.0.1', resolve));
  t.after(() => new Promise((resolve) => server.close(resolve)));
  const { port } = server.address();
  return `http://127.0.0.1:${port}`;
}

test('GET /health returns 200 with {"status":"ok"}', async (t) => {
  const baseUrl = await startServer(t);

  const res = await fetch(`${baseUrl}/health`);

  assert.equal(res.status, 200);
  assert.match(res.headers.get('content-type'), /^application\/json/);
  assert.deepEqual(await res.json(), { status: 'ok' });
});

test('unknown route returns 404', async (t) => {
  const baseUrl = await startServer(t);

  const res = await fetch(`${baseUrl}/nope`);

  assert.equal(res.status, 404);
  assert.deepEqual(await res.json(), { error: 'not found' });
});
