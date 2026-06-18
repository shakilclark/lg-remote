// Connection-state machine. Single source of truth for "are we talking to the TV?".
// Emits "change" with the full ConnectionState whenever anything changes.

import { EventEmitter } from "node:events";
import type { ConnectionState, ConnectionStatus } from "../types.js";

export class ConnectionStateMachine extends EventEmitter {
  private state: ConnectionState = { status: "disconnected", tvId: null };

  get(): ConnectionState {
    return { ...this.state };
  }

  /** Patch the state and emit "change" only if something actually changed. */
  update(patch: Partial<ConnectionState>): ConnectionState {
    const next: ConnectionState = { ...this.state, ...patch };
    // Clear volume/mute when we lose the connection so the UI doesn't show stale data.
    if (next.status !== "connected") {
      delete next.volume;
      delete next.muted;
    }
    if (!shallowEqual(next, this.state)) {
      this.state = next;
      this.emit("change", this.get());
    }
    return this.get();
  }

  setStatus(status: ConnectionStatus, message?: string): ConnectionState {
    return this.update({ status, message });
  }

  setTv(tvId: string | null): ConnectionState {
    return this.update({ tvId });
  }
}

function shallowEqual(a: ConnectionState, b: ConnectionState): boolean {
  return (
    a.status === b.status &&
    a.tvId === b.tvId &&
    a.volume === b.volume &&
    a.muted === b.muted &&
    a.message === b.message
  );
}
