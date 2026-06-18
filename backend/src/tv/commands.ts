// Maps app commands → SSAP requests on the live TV socket, and keeps volume/mute in the
// ConnectionState via a getVolume subscription. Contract: contracts/tv-protocol.md.

import type { TVClient } from "./client.js";
import type { ConnectionStateMachine } from "../state/connection.js";
import type { ControlCommand, ControlResult } from "../types.js";

type Conn = NonNullable<TVClient["raw"]>;
type Pointer = { send(type: string, payload?: Record<string, unknown>): void; close(): void };

const POINTER_URI = "ssap://com.webos.service.networkinput/getPointerInputSocket";

export class Commands {
  private lastPlaying = true; // best-effort; we don't get true media state for free
  private pointerPromise: Promise<Pointer> | null = null;

  constructor(
    private readonly client: TVClient,
    private readonly state: ConnectionStateMachine,
  ) {
    // (Re)subscribe to volume on every (re)connect so the UI mirrors the TV.
    this.client.onConnected = (conn) => {
      this.pointerPromise = null; // force re-acquire of the pointer socket after reconnect
      conn.subscribe("ssap://audio/getVolume", (err, res) => {
        if (err || !res) return;
        // webOS varies: newer nests under volumeStatus{volume,muteStatus}; older is flat.
        const vs = (res.volumeStatus ?? res) as Record<string, unknown>;
        const patch: { volume?: number; muted?: boolean } = {};
        if (typeof vs.volume === "number") patch.volume = vs.volume;
        if (typeof vs.muteStatus === "boolean") patch.muted = vs.muteStatus;
        else if (typeof res.muted === "boolean") patch.muted = res.muted;
        if (Object.keys(patch).length) this.state.update(patch);
      });
    };
  }

  async send(cmd: ControlCommand): Promise<ControlResult> {
    const conn = this.client.raw;
    if (!this.client.isConnected || !conn) {
      return { result: "failed", message: "TV not connected", state: this.state.get() };
    }
    try {
      switch (cmd.type) {
        case "volumeUp":
          await request(conn, "ssap://audio/volumeUp");
          break;
        case "volumeDown":
          await request(conn, "ssap://audio/volumeDown");
          break;
        case "setMute": {
          const mute = Boolean(cmd.params?.mute);
          await request(conn, "ssap://audio/setMute", { mute });
          this.state.update({ muted: mute });
          break;
        }
        case "playPause": {
          const play = !this.lastPlaying;
          await request(conn, play ? "ssap://media.controls/play" : "ssap://media.controls/pause");
          this.lastPlaying = play;
          break;
        }
        case "nav": {
          const button = cmd.params?.button;
          if (!button) return { result: "failed", message: "nav requires a button", state: this.state.get() };
          const pointer = await this.getPointer(conn);
          pointer.send("button", { name: button });
          break;
        }
        default:
          return { result: "failed", message: "Unsupported command", state: this.state.get() };
      }
      return { result: "acknowledged", state: this.state.get() };
    } catch (e) {
      return { result: "failed", message: (e as Error).message, state: this.state.get() };
    }
  }

  /** Acquire (and cache) the pointer-input socket used for D-pad/OK/Back/Home buttons. */
  private getPointer(conn: Conn): Promise<Pointer> {
    if (!this.pointerPromise) {
      this.pointerPromise = new Promise<Pointer>((resolve, reject) => {
        conn.getSocket(POINTER_URI, (err, sock) => {
          if (err || !sock) {
            this.pointerPromise = null;
            reject(err ?? new Error("Could not open pointer input socket"));
          } else {
            resolve(sock as Pointer);
          }
        });
      });
    }
    return this.pointerPromise;
  }
}

function request(conn: Conn, uri: string, payload?: Record<string, unknown>): Promise<unknown> {
  return new Promise((resolve, reject) => {
    conn.request(uri, payload ?? null, (err, res) => (err ? reject(err) : resolve(res)));
  });
}
