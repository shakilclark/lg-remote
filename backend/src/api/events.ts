// Read-only WebSocket that pushes ConnectionState to every connected UI client.
// Control never flows over this socket (that's POST /api/command); this is state-out only.
// Uses noServer mode; server.ts routes upgrades by path (one HTTP server, several WS paths).

import type { IncomingMessage } from "node:http";
import type { Duplex } from "node:stream";
import { WebSocketServer, WebSocket } from "ws";
import type { ConnectionStateMachine } from "../state/connection.js";

export class EventsHub {
  readonly path = "/api/events";
  private readonly wss = new WebSocketServer({ noServer: true });

  constructor(private readonly state: ConnectionStateMachine) {
    this.wss.on("connection", (ws) => {
      this.sendTo(ws, this.state.get()); // snapshot on connect so the UI renders truth
    });
    this.state.on("change", (s) => this.broadcast(s));
  }

  handleUpgrade(req: IncomingMessage, socket: Duplex, head: Buffer): void {
    this.wss.handleUpgrade(req, socket, head, (ws) => this.wss.emit("connection", ws, req));
  }

  private sendTo(ws: WebSocket, state: unknown): void {
    if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ type: "state", state }));
  }

  private broadcast(state: unknown): void {
    const msg = JSON.stringify({ type: "state", state });
    for (const ws of this.wss.clients) {
      if (ws.readyState === WebSocket.OPEN) ws.send(msg);
    }
  }

  close(): void {
    this.wss.close();
  }
}
