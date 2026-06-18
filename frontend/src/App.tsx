import { useEffect, useState } from "react";
import { useConnection } from "./state/useConnection";
import { ConnectionBanner } from "./components/ConnectionBanner";
import { ConnectScreen } from "./components/ConnectScreen";
import { VolumePad } from "./components/VolumePad";
import { PlaybackBar } from "./components/PlaybackBar";
import { DPad } from "./components/DPad";
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
      {connected ? <RemoteScreen volume={state.volume} muted={state.muted} /> : <ConnectScreen state={state} />}
    </div>
  );
}

// The full remote: volume + playback (US2) and D-pad navigation (US3).
function RemoteScreen({ volume, muted }: { volume?: number; muted?: boolean }) {
  return (
    <div className="screen remote">
      <div className="toprow">
        <VolumePad volume={volume} muted={muted} />
        <div className="sidecol">
          <PlaybackBar />
        </div>
      </div>
      <DPad />
    </div>
  );
}
