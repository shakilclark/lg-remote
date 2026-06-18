// Wires everything together: REST + events WS + static UI + auto-connect on boot.

import { createServer as createHTTPServer, type Server as HTTPServer } from "node:http";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";
import { existsSync } from "node:fs";
import express, { type Express, type NextFunction, type Request, type Response } from "express";
import { Store } from "./store/store.js";
import { ConnectionStateMachine } from "./state/connection.js";
import { TVClient, type TVClientDeps } from "./tv/client.js";
import { Commands } from "./tv/commands.js";
import { createRestRouter, type RestDeps } from "./api/rest.js";
import { EventsHub } from "./api/events.js";
import { CursorHub } from "./api/cursor.js";

export interface AppContext {
  app: Express;
  httpServer: HTTPServer;
  store: Store;
  state: ConnectionStateMachine;
  tvClient: TVClient;
  events: EventsHub;
  start(port?: number): Promise<number>;
  stop(): Promise<void>;
}

export interface BuildOptions {
  store?: Store;
  tvDeps?: TVClientDeps;
  discover?: RestDeps["discover"];
  serveStatic?: boolean;
  autoConnect?: boolean;
}

export function buildApp(opts: BuildOptions = {}): AppContext {
  const store = opts.store ?? new Store();
  const state = new ConnectionStateMachine();
  const tvClient = new TVClient(store, state, opts.tvDeps);
  const commands = new Commands(tvClient, state); // sets tvClient.onConnected before any connect

  const app = express();
  app.use(express.json());
  app.use("/api", createRestRouter({ store, state, tvClient, commands, discover: opts.discover }));

  // Serve the built PWA when present (production: frontend build → backend/public).
  if (opts.serveStatic !== false) {
    const here = dirname(fileURLToPath(import.meta.url));
    const publicDir = join(here, "..", "public");
    if (existsSync(publicDir)) {
      app.use(express.static(publicDir));
      app.get(/^(?!\/api).*/, (_req, res) => res.sendFile(join(publicDir, "index.html")));
    }
  }

  // Consistent error envelope: { message } (contract).
  app.use((err: unknown, _req: Request, res: Response, _next: NextFunction) => {
    const message = err instanceof Error ? err.message : "Internal error";
    res.status(500).json({ message });
  });

  const httpServer = createHTTPServer(app);
  const events = new EventsHub(state);
  const cursor = new CursorHub(commands);

  // One upgrade handler routes to the right WS hub by path (avoids ws path-claim conflicts).
  httpServer.on("upgrade", (req, socket, head) => {
    const pathname = (req.url ?? "").split("?")[0];
    if (pathname === events.path) events.handleUpgrade(req, socket, head);
    else if (pathname === cursor.path) cursor.handleUpgrade(req, socket, head);
    else socket.destroy();
  });

  if (opts.autoConnect !== false && store.getActive()) {
    tvClient.pairActive();
  }

  return {
    app,
    httpServer,
    store,
    state,
    tvClient,
    events,
    start(port = Number(process.env.PORT ?? 8080)) {
      return new Promise((resolve) => {
        httpServer.listen(port, () => {
          const addr = httpServer.address();
          resolve(typeof addr === "object" && addr ? addr.port : port);
        });
      });
    },
    stop() {
      return new Promise((resolve) => {
        events.close();
        cursor.close();
        tvClient.disconnect();
        httpServer.close(() => resolve());
      });
    },
  };
}

// Entry point (skipped when imported by tests).
const isMain = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1];
if (isMain) {
  const ctx = buildApp();
  ctx.start().then((port) => {
    // eslint-disable-next-line no-console
    console.log(`lg-remote backend listening on http://0.0.0.0:${port}`);
  });
}
