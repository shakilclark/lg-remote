// lgtv2 wrapper: owns the single live socket to the active TV.
// Maps lgtv2 events → ConnectionStateMachine, persists the pairing key, auto-reconnects,
// and recovers from the TV's IP changing (rediscovery after repeated failures — F1).

import lgtv from "lgtv2";
import { homedir } from "node:os";
import { dirname, join } from "node:path";
import type { Store } from "../store/store.js";
import type { ConnectionStateMachine } from "../state/connection.js";
import type { TVConnection } from "../types.js";
import { discoverTVs, type DiscoveredTV } from "./discovery.js";

const RECONNECT_MS = 5000;
const REDISCOVER_AFTER_FAILURES = 3;

export interface TVClientDeps {
  createLgtv?: typeof lgtv;
  discover?: (timeoutMs?: number) => Promise<DiscoveredTV[]>;
}

type LgtvInstance = ReturnType<typeof lgtv>;

function keyFilePath(tvId: string | null): string {
  const base = process.env.LG_REMOTE_STORE
    ? dirname(process.env.LG_REMOTE_STORE)
    : join(homedir(), ".config", "lg-remote");
  const safe = (tvId ?? "default").replace(/[^a-z0-9._-]/gi, "_");
  return join(base, `lgtv2-keyfile-${safe}`);
}

export class TVClient {
  private conn: LgtvInstance | null = null;
  private activeTvId: string | null = null;
  private failures = 0;
  private rediscovering = false;
  private readonly createLgtv: typeof lgtv;
  private readonly discover: (timeoutMs?: number) => Promise<DiscoveredTV[]>;

  constructor(
    private readonly store: Store,
    private readonly state: ConnectionStateMachine,
    deps: TVClientDeps = {},
  ) {
    this.createLgtv = deps.createLgtv ?? lgtv;
    this.discover = deps.discover ?? discoverTVs;
  }

  /** The live lgtv2 instance, when connected — used by the command layer (US2/US3). */
  get raw(): LgtvInstance | null {
    return this.conn;
  }

  get isConnected(): boolean {
    return this.state.get().status === "connected";
  }

  /** Connect to (and begin pairing/auto-reconnecting) the given TV. */
  connectTo(tv: TVConnection): void {
    this.teardown();
    this.activeTvId = tv.id;
    this.failures = 0;
    this.store.setActive(tv.id);
    this.state.setTv(tv.id);
    this.state.setStatus("connecting");
    this.open(tv.address, tv.clientKey);
  }

  /** Ensure the active TV (from the store) is being connected. */
  pairActive(): void {
    const tv = this.store.getActive();
    if (!tv) {
      this.state.setStatus("disconnected", "No TV selected");
      return;
    }
    if (this.activeTvId !== tv.id || !this.conn) this.connectTo(tv);
  }

  disconnect(): void {
    this.teardown();
    this.state.setStatus("disconnected");
  }

  private open(address: string, clientKey: string | null): void {
    const tvId = this.activeTvId;
    const conn = this.createLgtv({
      url: `ws://${address}:3000`,
      reconnect: RECONNECT_MS,
      clientKey: clientKey ?? undefined,
      // App-owned key-file so we never read another app's default ~/.lgtv2 key.
      keyFile: keyFilePath(tvId),
      saveKey: (key, cb) => {
        if (tvId) this.store.setClientKey(tvId, key);
        cb();
      },
    });
    this.conn = conn;

    conn.on("connecting", () => {
      if (!this.isConnected) this.state.setStatus("connecting");
    });

    conn.on("prompt", () => {
      this.state.setStatus("needs-pairing", "Accept the prompt on your TV");
    });

    conn.on("connect", () => {
      this.failures = 0;
      if (tvId) this.store.markConnected(tvId, new Date().toISOString());
      this.state.setStatus("connected");
      this.onConnected?.(conn);
    });

    conn.on("error", (err: NodeJS.ErrnoException) =>
      this.handleDrop(err?.message, err?.code),
    );
    conn.on("close", () => this.handleDrop());
  }

  /** Hook the command layer (US2/US3) sets to (re)subscribe on every (re)connect. */
  onConnected?: (conn: LgtvInstance) => void;

  // Socket error codes that mean "no network path to the TV" rather than "TV refused/closed".
  private static readonly OFF_NETWORK_CODES = new Set([
    "EHOSTUNREACH",
    "ENETUNREACH",
    "EHOSTDOWN",
    "ENETDOWN",
    "EAI_AGAIN", // DNS/resolver unavailable — typically off-network
  ]);

  private handleDrop(message?: string, code?: string): void {
    const offNetwork = code != null && TVClient.OFF_NETWORK_CODES.has(code);
    // Reflect the truth on every drop (not only from "connected"): the reconnect loop will
    // flip back to "connecting" on its next attempt, so the UI never lies (Principle IV).
    this.state.setStatus(
      offNetwork ? "off-network" : "disconnected",
      offNetwork ? "Not on the same network as your TV" : message ?? "Connection lost",
    );
    this.failures += 1;
    if (this.failures >= REDISCOVER_AFTER_FAILURES) {
      void this.tryRediscover();
    }
  }

  /** F1: after repeated failures, rescan in case the TV's IP changed (DHCP). */
  private async tryRediscover(): Promise<void> {
    if (this.rediscovering || !this.activeTvId) return;
    this.rediscovering = true;
    try {
      const tv = this.store.get(this.activeTvId);
      if (!tv) return;
      const found = await this.discover();
      const match = found.find((d) => d.id === tv.id);
      if (match && match.address !== tv.address) {
        this.store.setAddress(tv.id, match.address);
        this.failures = 0;
        this.teardown();
        this.open(match.address, this.store.get(tv.id)?.clientKey ?? null);
      }
    } catch {
      // best-effort; lgtv2's own reconnect loop keeps retrying the old address meanwhile
    } finally {
      this.rediscovering = false;
    }
  }

  private teardown(): void {
    if (this.conn) {
      this.conn.removeAllListeners();
      // Keep an error sink: lgtv2 may emit a late socket error (e.g. ECONNRESET) after we
      // stop listening; without a handler Node would throw on the 'error' event.
      this.conn.on("error", () => {});
      try {
        this.conn.disconnect();
      } catch {
        /* ignore */
      }
      this.conn = null;
    }
  }
}
