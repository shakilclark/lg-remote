import { useEffect, useState } from "react";
import { useConnection } from "./state/useConnection";
import { ConnectionBanner } from "./components/ConnectionBanner";
import { ConnectScreen } from "./components/ConnectScreen";
import { api } from "./api/client";

export default function App() {
  const state = useConnection();
  const [name, setName] = useState<string | undefined>();

  // Track the active TV's friendly name for the banner.
  useEffect(() => {
    api
      .getTvs()
      .then(({ tvs, activeId }) => setName(tvs.find((t) => t.id === activeId)?.name))
      .catch(() => {});
  }, [state.tvId, state.status]);

  const connected = state.status === "connected";

  return (
    <div className="app">
      <ConnectionBanner state={state} name={name} />
      {connected ? <ConnectedView volume={state.volume} muted={state.muted} /> : <ConnectScreen state={state} />}
    </div>
  );
}

// Placeholder for the connected state. Volume/playback (US2) and the D-pad (US3) land in
// the next slices; this confirms the end-to-end connection is live.
function ConnectedView({ volume, muted }: { volume?: number; muted?: boolean }) {
  return (
    <div className="screen connected">
      <div className="hero">
        <div className="bigemoji">✅</div>
        <h1>Connected</h1>
        <p>Your TV is paired and reachable.</p>
      </div>
      <div className="statline">
        Volume <b>{volume ?? "—"}</b>
        {muted ? " · muted" : ""}
      </div>
      <div className="comingsoon">Remote controls arrive in the next build slice.</div>
    </div>
  );
}
