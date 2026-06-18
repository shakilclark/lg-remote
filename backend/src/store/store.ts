// JSON persistence of remembered TVs + pairing keys.
// Location: $LG_REMOTE_STORE or ~/.config/lg-remote/store.json
// The clientKey is sensitive and never leaves this process boundary except to lgtv2.

import { homedir } from "node:os";
import { join, dirname } from "node:path";
import { mkdirSync, readFileSync, writeFileSync, existsSync } from "node:fs";
import type { TVConnection } from "../types.js";

interface StoreShape {
  tvs: TVConnection[];
  activeId: string | null;
}

const EMPTY: StoreShape = { tvs: [], activeId: null };

function storePath(): string {
  if (process.env.LG_REMOTE_STORE) return process.env.LG_REMOTE_STORE;
  return join(homedir(), ".config", "lg-remote", "store.json");
}

export class Store {
  private data: StoreShape;
  private readonly path: string;

  constructor(path = storePath()) {
    this.path = path;
    this.data = this.load();
  }

  private load(): StoreShape {
    try {
      if (!existsSync(this.path)) return structuredClone(EMPTY);
      const raw = readFileSync(this.path, "utf8");
      const parsed = JSON.parse(raw) as Partial<StoreShape>;
      return { tvs: parsed.tvs ?? [], activeId: parsed.activeId ?? null };
    } catch {
      // Corrupt/unreadable store should not crash the agent — start fresh.
      return structuredClone(EMPTY);
    }
  }

  private persist(): void {
    mkdirSync(dirname(this.path), { recursive: true });
    writeFileSync(this.path, JSON.stringify(this.data, null, 2), "utf8");
  }

  list(): TVConnection[] {
    return this.data.tvs.map((t) => ({ ...t }));
  }

  get(id: string): TVConnection | undefined {
    const t = this.data.tvs.find((x) => x.id === id);
    return t ? { ...t } : undefined;
  }

  getActive(): TVConnection | undefined {
    return this.data.activeId ? this.get(this.data.activeId) : undefined;
  }

  get activeId(): string | null {
    return this.data.activeId;
  }

  /** Insert or update a TV (matched by id). Does not change activeId. */
  upsert(tv: TVConnection): TVConnection {
    const idx = this.data.tvs.findIndex((x) => x.id === tv.id);
    if (idx >= 0) this.data.tvs[idx] = { ...tv };
    else this.data.tvs.push({ ...tv });
    this.persist();
    return { ...tv };
  }

  setActive(id: string | null): void {
    this.data.activeId = id;
    this.persist();
  }

  /** Persist a freshly issued pairing key for a TV. */
  setClientKey(id: string, clientKey: string): void {
    const tv = this.data.tvs.find((x) => x.id === id);
    if (!tv) return;
    tv.clientKey = clientKey;
    this.persist();
  }

  /** Update a TV's last-known address (e.g. after DHCP change / rediscovery). */
  setAddress(id: string, address: string): void {
    const tv = this.data.tvs.find((x) => x.id === id);
    if (!tv || tv.address === address) return;
    tv.address = address;
    this.persist();
  }

  markConnected(id: string, when: string): void {
    const tv = this.data.tvs.find((x) => x.id === id);
    if (!tv) return;
    tv.lastConnectedAt = when;
    this.persist();
  }
}
