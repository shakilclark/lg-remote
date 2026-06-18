// Low-latency cursor input over a dedicated WebSocket (US5 motion cursor).
// The phone streams {type:"move",dx,dy} frames (and {type:"click"}) while the cursor button
// is held; we forward them to the TV's pointer-input socket. REST-per-frame would be too slow.

import type { IncomingMessage } from "node:http";
import type { Duplex } from "node:stream";
import { WebSocketServer } from "ws";
import type { Commands } from "../tv/commands.js";

export class CursorHub {
  readonly path = "/api/cursor";
  private readonly wss = new WebSocketServer({ noServer: true });

  constructor(commands: Commands) {
    this.wss.on("connection", (ws) => {
      ws.on("message", (data) => {
        let msg: { type?: string; dx?: number; dy?: number };
        try {
          msg = JSON.parse(String(data));
        } catch {
          return;
        }
        if (msg.type === "move") {
          void commands.cursorMove(Number(msg.dx) || 0, Number(msg.dy) || 0);
        } else if (msg.type === "click") {
          void commands.cursorClick();
        }
      });
      ws.on("error", () => {});
    });
  }

  handleUpgrade(req: IncomingMessage, socket: Duplex, head: Buffer): void {
    this.wss.handleUpgrade(req, socket, head, (ws) => this.wss.emit("connection", ws, req));
  }

  close(): void {
    this.wss.close();
  }
}
