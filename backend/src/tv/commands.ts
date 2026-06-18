// Maps app commands → SSAP requests on the live TV socket, and keeps volume/mute in the
// ConnectionState via a getVolume subscription. Contract: contracts/tv-protocol.md.

import type { TVClient } from "./client.js";
import type { ConnectionStateMachine } from "../state/connection.js";
import type { ControlCommand, ControlResult } from "../types.js";

type Conn = NonNullable<TVClient["raw"]>;

export class Commands {
  private lastPlaying = true; // best-effort; we don't get true media state for free

  constructor(
    private readonly client: TVClient,
    private readonly state: ConnectionStateMachine,
  ) {
    // (Re)subscribe to volume on every (re)connect so the UI mirrors the TV.
    this.client.onConnected = (conn) => {
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
        default:
          return { result: "failed", message: "Unsupported command", state: this.state.get() };
      }
      return { result: "acknowledged", state: this.state.get() };
    } catch (e) {
      return { result: "failed", message: (e as Error).message, state: this.state.get() };
    }
  }
}

function request(conn: Conn, uri: string, payload?: Record<string, unknown>): Promise<unknown> {
  return new Promise((resolve, reject) => {
    conn.request(uri, payload ?? null, (err, res) => (err ? reject(err) : resolve(res)));
  });
}
