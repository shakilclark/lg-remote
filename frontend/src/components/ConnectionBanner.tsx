import type { ConnectionState, ConnectionStatus } from "../api/client";

const LABELS: Record<ConnectionStatus, string> = {
  "needs-pairing": "Accept the prompt on your TV",
  connecting: "Connecting…",
  connected: "Connected",
  disconnected: "Disconnected",
  "off-network": "Not on the same network as your TV",
};

const DOT: Record<ConnectionStatus, string> = {
  "needs-pairing": "warn",
  connecting: "connecting",
  connected: "ok",
  disconnected: "off",
  "off-network": "off",
};

export function ConnectionBanner({ state, name }: { state: ConnectionState; name?: string }) {
  const sub = state.message ?? LABELS[state.status];
  return (
    <div className="banner">
      <span className={`dot ${DOT[state.status]}`} />
      <div>
        <div className="title">{name ?? "LG TV"}</div>
        <div className="sub">{sub}</div>
      </div>
    </div>
  );
}
