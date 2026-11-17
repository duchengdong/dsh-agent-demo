import { createServer } from 'node:http';
import { createRequestHandler } from './app.js';

const port = Number(process.env.PORT ?? 3000);
const host = process.env.HOST ?? '127.0.0.1';

const server = createServer(createRequestHandler());

server.listen(port, host, () => {
  console.log(`Server listening on http://${host}:${port}`);
});

for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => {
    server.close(() => process.exit(0));
  });
}
