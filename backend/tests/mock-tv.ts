// Mock webOS TV: a real WebSocket server speaking enough SSAP for lgtv2.
// Protocol (from lgtv2): client sends {id,type:'register',payload}. We route replies by id.
//   - register WITHOUT payload['client-key']  -> reply prompt (no key) then auto-accept with a key
//   - register WITH a client-key              -> reply 'registered' immediately (already paired)
//   - request{uri}                            -> reply {id,payload:{returnValue:true,...}}

import { WebSocketServer, type WebSocket } from "ws";
import type { AddressInfo } from "node:net";

export interface MockTVOptions {
  /** ms before the "user accepts" the on-TV prompt. */
  autoAcceptMs?: number;
  /** the key the TV hands out / recognises. */
  clientKey?: string;
}

export class MockTV {
  private wss: WebSocketServer | null = null;
  readonly clientKey: string;
  private readonly autoAcceptMs: number;
  registerCount = 0;
  promptCount = 0;
  lastRequests: string[] = [];

  constructor(opts: MockTVOptions = {}) {
    this.clientKey = opts.clientKey ?? "MOCK-CLIENT-KEY";
    this.autoAcceptMs = opts.autoAcceptMs ?? 80;
  }

  start(): Promise<string> {
    return new Promise((resolve) => {
      const wss = new WebSocketServer({ port: 0 }, () => {
        const { port } = wss.address() as AddressInfo;
        resolve(`ws://127.0.0.1:${port}`);
      });
      this.wss = wss;
      wss.on("connection", (ws) => this.onConnection(ws));
    });
  }

  private onConnection(ws: WebSocket): void {
    ws.on("error", () => {}); // swallow resets during teardown
    ws.on("message", (data) => {
      let msg: { id?: string; type?: string; uri?: string; payload?: Record<string, unknown> };
      try {
        msg = JSON.parse(String(data));
      } catch {
        return;
      }
      const { id, type, uri, payload } = msg;
      if (type === "register") {
        this.registerCount += 1;
        const presentedKey = payload?.["client-key"];
        if (presentedKey) {
          this.reply(ws, id, { "client-key": presentedKey });
        } else {
          this.promptCount += 1;
          // First: a prompt response with no key -> lgtv2 emits 'prompt'.
          this.reply(ws, id, { pairingType: "PROMPT", returnValue: true });
          // Then: simulate the user accepting on the TV.
          setTimeout(() => this.reply(ws, id, { "client-key": this.clientKey }), this.autoAcceptMs);
        }
        return;
      }
      if (type === "request" && uri) {
        this.lastRequests.push(uri);
        this.reply(ws, id, this.responseFor(uri));
      }
    });
  }

  private responseFor(uri: string): Record<string, unknown> {
    if (uri.includes("audio/getVolume")) return { returnValue: true, volume: 13, muted: false };
    if (uri.includes("getPointerInputSocket")) {
      const { port } = this.wss!.address() as AddressInfo;
      return { returnValue: true, socketPath: `ws://127.0.0.1:${port}` };
    }
    return { returnValue: true };
  }

  private reply(ws: WebSocket, id: string | undefined, payload: Record<string, unknown>): void {
    if (ws.readyState !== ws.OPEN) return;
    ws.send(JSON.stringify({ type: "response", id, payload }));
  }

  stop(): Promise<void> {
    return new Promise((resolve) => {
      if (!this.wss) return resolve();
      for (const c of this.wss.clients) c.terminate();
      this.wss.close(() => resolve());
      this.wss = null;
    });
  }
}
