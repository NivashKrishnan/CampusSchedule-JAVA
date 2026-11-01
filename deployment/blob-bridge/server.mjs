import http from 'node:http';
import { get, put, BlobPreconditionFailedError } from '@vercel/blob';

// Opaque data only. Java performs authentication, validation and serialization.
// This service is loopback-only and must never be reverse-proxied to the Internet.
// Preview deployments must not edit the production workspace.
const environment = process.env.VERCEL_ENV === 'production' ? 'production' : 'preview';
const path = `campusschedule/${environment}/workspace-v1.bin`;
const maxBytes = 4 * 1024 * 1024;
const server = http.createServer(async (req, res) => {
  res.setHeader('Cache-Control', 'no-store');
  if (req.url !== '/state' || !['GET', 'PUT'].includes(req.method)) {
    res.writeHead(404).end(); return;
  }
  const oidcToken = req.headers['x-vercel-oidc-token'] || process.env.VERCEL_OIDC_TOKEN;
  if (!oidcToken || !process.env.BLOB_STORE_ID) { res.writeHead(503).end(); return; }
  const auth = { access: 'private', storeId: process.env.BLOB_STORE_ID, oidcToken };
  try {
    if (req.method === 'GET') {
      const value = await get(path, { ...auth, useCache: false });
      if (!value) { res.writeHead(404).end(); return; }
      if (value.statusCode !== 200 || value.blob.size > maxBytes) { res.writeHead(502).end(); return; }
      res.setHeader('ETag', value.blob.etag);
      res.setHeader('Content-Type', 'application/octet-stream');
      const buffer = Buffer.from(await new Response(value.stream).arrayBuffer());
      if (buffer.length > maxBytes) { res.writeHead(502).end(); return; }
      res.end(buffer);
    } else {
      const chunks = []; let size = 0;
      for await (const chunk of req) {
        size += chunk.length;
        if (size > maxBytes) { res.writeHead(413).end(); return; }
        chunks.push(chunk);
      }
      const revision = req.headers['if-match'];
      // An absent version means create-only. It must never overwrite another owner setup.
      const value = await put(path, Buffer.concat(chunks), {
        ...auth, contentType: 'application/octet-stream', addRandomSuffix: false,
        allowOverwrite: Boolean(revision), ...(revision ? { ifMatch: revision } : {}),
      });
      res.setHeader('ETag', value.etag);
      res.writeHead(200).end();
    }
  } catch (error) {
    // Do not log SDK errors: their metadata can include authentication context.
    const conflict = error instanceof BlobPreconditionFailedError;
    console.error('Private storage request failed:', error?.constructor?.name || 'UnknownError');
    res.writeHead(conflict ? 409 : 503).end();
  }
});
server.requestTimeout = 25000;
server.headersTimeout = 10000;
server.listen(8181, '127.0.0.1');
