// Maps app commands → SSAP requests on the live TV socket, and keeps volume/mute in the
// ConnectionState via a getVolume subscription. Contract: contracts/tv-protocol.md.

import type { TVClient } from "./client.js";
import type { ConnectionStateMachine } from "../state/connection.js";
import type { AppKey, ControlCommand, ControlResult, TVInput } from "../types.js";

type Conn = NonNullable<TVClient["raw"]>;
type Pointer = { send(type: string, payload?: Record<string, unknown>): void; close(): void };

const POINTER_URI = "ssap://com.webos.service.networkinput/getPointerInputSocket";
const LIST_APPS_URI = "ssap://com.webos.applicationManager/listLaunchPoints";
const LAUNCH_URI = "ssap://system.launcher/launch";
const LIST_INPUTS_URI = "ssap://tv/getExternalInputList";
const SWITCH_INPUT_URI = "ssap://tv/switchInput";

// Fallback ids when listLaunchPoints can't be matched (varies by webOS version).
const WELL_KNOWN_APP_IDS: Record<AppKey, string> = {
  youtube: "youtube.leanback.v4",
  netflix: "netflix",
};

export class Commands {
  private lastPlaying = true; // best-effort; we don't get true media state for free
  private pointerPromise: Promise<Pointer> | null = null;
  private launchPoints: Array<{ id: string; title: string }> = [];

  constructor(
    private readonly client: TVClient,
    private readonly state: ConnectionStateMachine,
  ) {
    // (Re)subscribe to volume on every (re)connect so the UI mirrors the TV.
    this.client.onConnected = (conn) => {
      this.pointerPromise = null; // force re-acquire of the pointer socket after reconnect
      this.launchPoints = [];
      // Cache the TV's installed apps so we can resolve shortcut ids by title.
      conn.request(LIST_APPS_URI, null, (err, res) => {
        if (!err && Array.isArray(res?.launchPoints)) {
          this.launchPoints = res.launchPoints
            .filter((p: { id?: string; title?: string }) => p.id && p.title)
            .map((p: { id: string; title: string }) => ({ id: p.id, title: p.title }));
        }
      });
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
        case "launchApp": {
          const app = cmd.params?.app;
          if (!app) return { result: "failed", message: "launchApp requires an app", state: this.state.get() };
          const id = this.resolveAppId(app);
          const res = (await request(conn, LAUNCH_URI, { id })) as { returnValue?: boolean } | undefined;
          if (res && res.returnValue === false) {
            return { result: "failed", message: `${app} isn't available on this TV`, state: this.state.get() };
          }
          break;
        }
        case "setInput": {
          const inputId = cmd.params?.inputId;
          if (!inputId) return { result: "failed", message: "setInput requires an inputId", state: this.state.get() };
          const res = (await request(conn, SWITCH_INPUT_URI, { inputId })) as { returnValue?: boolean } | undefined;
          if (res && res.returnValue === false) {
            return { result: "failed", message: "Couldn't switch input", state: this.state.get() };
          }
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

  /** List the TV's external inputs/sources (US7). Empty when not connected. */
  async listInputs(): Promise<TVInput[]> {
    const conn = this.client.raw;
    if (!this.client.isConnected || !conn) return [];
    const res = (await request(conn, LIST_INPUTS_URI)) as
      | { devices?: Array<{ id?: string; label?: string }> }
      | undefined;
    return (res?.devices ?? [])
      .filter((d): d is { id: string; label?: string } => typeof d.id === "string")
      .map((d) => ({ id: d.id, label: d.label ?? d.id }));
  }

  /** Resolve a shortcut app to its TV app id: match a launch-point title, else fall back. */
  private resolveAppId(app: AppKey): string {
    const match = this.launchPoints.find((p) => p.title.toLowerCase().includes(app));
    return match?.id ?? WELL_KNOWN_APP_IDS[app];
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
