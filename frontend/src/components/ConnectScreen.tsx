import { useState } from "react";
import { api, type DiscoveredTV, type ConnectionState } from "../api/client";

export function ConnectScreen({ state }: { state: ConnectionState }) {
  const [found, setFound] = useState<DiscoveredTV[]>([]);
  const [scanning, setScanning] = useState(false);
  const [manual, setManual] = useState("");
  const [error, setError] = useState<string | null>(null);

  const pairing = state.status === "needs-pairing";

  async function scan() {
    setScanning(true);
    setError(null);
    try {
      const { found } = await api.discover();
      setFound(found);
      if (found.length === 0) setError("No TVs found. Enter the IP below.");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setScanning(false);
    }
  }

  async function connect(address: string, name?: string) {
    setError(null);
    try {
      await api.addTv(address, name);
      await api.pair();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="screen">
      <div className="hero">
        <div className="bigemoji">📺</div>
        <h1>{pairing ? "Almost there…" : "Find your TV"}</h1>
        <p>
          {pairing
            ? "Look at your TV and accept the connection request."
            : "Make sure your LG TV is on and on the same Wi-Fi."}
        </p>
      </div>

      {found.length > 0 && <div className="cardlabel">Found on your network</div>}
      {found.map((tv) => (
        <button key={tv.id} className="tvitem" onClick={() => connect(tv.address, tv.name)}>
          <span className="ico">📺</span>
          <span className="meta">
            <b>{tv.name}</b>
            <span>LG webOS · {tv.address}</span>
          </span>
          <span className="chev">›</span>
        </button>
      ))}

      {error && <div className="error">{error}</div>}

      <div className="manualrow">
        <input
          className="ipinput"
          inputMode="decimal"
          placeholder="Enter TV IP (e.g. 192.168.0.50)"
          value={manual}
          onChange={(e) => setManual(e.target.value)}
        />
        <button
          className="btn small"
          disabled={!manual.trim()}
          onClick={() => connect(manual.trim())}
        >
          Add
        </button>
      </div>

      <div className="spacer" />
      <button className="btn cta" onClick={scan} disabled={scanning}>
        {scanning ? "Scanning…" : found.length ? "Scan again" : "Scan for TVs"}
      </button>
    </div>
  );
}
