// Read-only WebSocket that pushes ConnectionState to every connected UI client.
// Control never flows over this socket (that's POST /api/command); this is state-out only.

import type { Server as HTTPServer } from "node:http";
import { WebSocketServer, WebSocket } from "ws";
import type { ConnectionStateMachine } from "../state/connection.js";

export class EventsHub {
  private readonly wss: WebSocketServer;

  constructor(server: HTTPServer, private readonly state: ConnectionStateMachine) {
    this.wss = new WebSocketServer({ server, path: "/api/events" });

    this.wss.on("connection", (ws) => {
      // Send a snapshot immediately so the UI renders the truth on connect.
      this.sendTo(ws, this.state.get());
    });

    this.state.on("change", (s) => this.broadcast(s));
  }

  private sendTo(ws: WebSocket, state: unknown): void {
    if (ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: "state", state }));
    }
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
