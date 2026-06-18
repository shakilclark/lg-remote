// Talks to the backend REST + events WS. Shapes mirror contracts/backend-api.md.

export type ConnectionStatus =
  | "needs-pairing"
  | "connecting"
  | "connected"
  | "disconnected"
  | "off-network";

export interface ConnectionState {
  status: ConnectionStatus;
  tvId: string | null;
  volume?: number;
  muted?: boolean;
  message?: string;
}

export interface TVPublic {
  id: string;
  name: string;
  address: string;
  paired: boolean;
}

export interface DiscoveredTV {
  id: string;
  name: string;
  address: string;
}

async function json<T>(res: Response): Promise<T> {
  if (!res.ok) {
    const body = (await res.json().catch(() => ({}))) as { message?: string };
    throw new Error(body.message ?? `Request failed (${res.status})`);
  }
  return res.json() as Promise<T>;
}

export const api = {
  getState: () => fetch("/api/state").then(json<ConnectionState>),
  getTvs: () => fetch("/api/tvs").then(json<{ tvs: TVPublic[]; activeId: string | null }>),
  discover: () =>
    fetch("/api/discover", { method: "POST" }).then(json<{ found: DiscoveredTV[] }>),
  addTv: (address: string, name?: string) =>
    fetch("/api/tvs", {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ address, name }),
    }).then(json<{ id: string; activeId: string | null }>),
  pair: () =>
    fetch("/api/pair", { method: "POST" }).then(
      json<{ status: ConnectionStatus; message?: string }>,
    ),
};

/** Subscribe to live connection-state pushes. Returns an unsubscribe fn. Auto-reconnects. */
export function subscribeState(onState: (s: ConnectionState) => void): () => void {
  let ws: WebSocket | null = null;
  let closed = false;
  let retry: ReturnType<typeof setTimeout> | null = null;

  const connect = () => {
    const proto = location.protocol === "https:" ? "wss" : "ws";
    ws = new WebSocket(`${proto}://${location.host}/api/events`);
    ws.onmessage = (ev) => {
      try {
        const msg = JSON.parse(ev.data) as { type: string; state: ConnectionState };
        if (msg.type === "state") onState(msg.state);
      } catch {
        /* ignore malformed */
      }
    };
    ws.onclose = () => {
      if (closed) return;
      retry = setTimeout(connect, 1500);
    };
    ws.onerror = () => ws?.close();
  };
  connect();

  return () => {
    closed = true;
    if (retry) clearTimeout(retry);
    ws?.close();
  };
}
