import { useEffect, useState } from "react";
import { useConnection } from "./state/useConnection";
import { ConnectionBanner } from "./components/ConnectionBanner";
import { ConnectScreen } from "./components/ConnectScreen";
import { VolumePad } from "./components/VolumePad";
import { PlaybackBar } from "./components/PlaybackBar";
import { DPad } from "./components/DPad";
import { AppShortcuts } from "./components/AppShortcuts";
import { InputSwitcher } from "./components/InputSwitcher";
import { api } from "./api/client";

export default function App() {
  const state = useConnection();
  const [name, setName] = useState<string | undefined>();
  const [hasActiveTv, setHasActiveTv] = useState(false);
  const [reconfigure, setReconfigure] = useState(false);

  useEffect(() => {
    api
      .getTvs()
      .then(({ tvs, activeId }) => {
        setHasActiveTv(!!activeId);
        setName(tvs.find((t) => t.id === activeId)?.name);
      })
      .catch(() => {});
  }, [state.tvId, state.status]);

  const connected = state.status === "connected";
  // A known TV that's just (re)connecting/dropped → show a reconnect view, not the scanner.
  const transient = ["connecting", "disconnected", "off-network"].includes(state.status);
  const showReconnect = !connected && hasActiveTv && transient && !reconfigure;

  let body;
  if (connected) body = <RemoteScreen volume={state.volume} muted={state.muted} />;
  else if (showReconnect) body = <ReconnectView onChange={() => setReconfigure(true)} />;
  else body = <ConnectScreen state={state} />;

  return (
    <div className="app">
      <ConnectionBanner state={state} name={name} />
      {body}
    </div>
  );
}

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
      <AppShortcuts />
      <InputSwitcher />
    </div>
  );
}

function ReconnectView({ onChange }: { onChange: () => void }) {
  const [trying, setTrying] = useState(false);
  const retry = async () => {
    setTrying(true);
    try {
      await api.pair();
    } finally {
      setTrying(false);
    }
  };
  return (
    <div className="screen">
      <div className="hero">
        <div className="bigemoji">📺</div>
        <h1>Can't reach your TV</h1>
        <p>It may be off or asleep. It'll reconnect automatically when it's back.</p>
      </div>
      <div className="spacer" />
      <button className="btn cta" onClick={retry} disabled={trying}>
        {trying ? "Reconnecting…" : "Try now"}
      </button>
      <button className="btn small full" onClick={onChange}>
        Choose a different TV
      </button>
    </div>
  );
}
