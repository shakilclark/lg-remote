import { useEffect, useRef, useState } from "react";
import { buzz } from "../api/client";
import { openCursor, type CursorConn } from "../api/cursor";

// How many pointer units per degree of phone tilt, and the jitter dead-zone (degrees).
const SENSITIVITY = 14;
const DEAD_ZONE = 0.25;

type OrientationPermission = { requestPermission?: () => Promise<"granted" | "denied"> };

export function CursorPad() {
  const conn = useRef<CursorConn | null>(null);
  const last = useRef<{ beta: number; gamma: number } | null>(null);
  const [holding, setHolding] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Open the cursor socket while this pad is mounted (i.e. while connected).
  useEffect(() => {
    conn.current = openCursor();
    return () => conn.current?.close();
  }, []);

  const onOrientation = (e: DeviceOrientationEvent) => {
    if (e.beta == null || e.gamma == null) return;
    const prev = last.current;
    last.current = { beta: e.beta, gamma: e.gamma };
    if (!prev) return; // first sample establishes the reference
    const dGamma = e.gamma - prev.gamma; // left/right tilt → x
    const dBeta = e.beta - prev.beta; // front/back tilt → y
    const dx = Math.abs(dGamma) > DEAD_ZONE ? dGamma * SENSITIVITY : 0;
    const dy = Math.abs(dBeta) > DEAD_ZONE ? dBeta * SENSITIVITY : 0;
    if (dx || dy) conn.current?.move(dx, dy);
  };

  const startHold = async () => {
    setError(null);
    // iOS requires explicit permission, requested from a user gesture, over HTTPS.
    const DOE = window.DeviceOrientationEvent as unknown as OrientationPermission | undefined;
    if (DOE && typeof DOE.requestPermission === "function") {
      try {
        const res = await DOE.requestPermission();
        if (res !== "granted") {
          setError("Motion access denied — enable it in Settings to use the cursor.");
          return;
        }
      } catch {
        setError("Motion needs a secure (HTTPS) connection — use the Tailscale URL.");
        return;
      }
    }
    last.current = null;
    setHolding(true);
    buzz();
    window.addEventListener("deviceorientation", onOrientation);
  };

  const stopHold = () => {
    setHolding(false);
    window.removeEventListener("deviceorientation", onOrientation);
  };

  useEffect(() => () => window.removeEventListener("deviceorientation", onOrientation), []);

  return (
    <div className="cursorpad">
      <div className="cardlabel">Magic cursor</div>
      <div className="cursorrow">
        <button
          className={`btn cursorhold ${holding ? "active" : ""}`}
          onPointerDown={startHold}
          onPointerUp={stopHold}
          onPointerLeave={stopHold}
          onContextMenu={(e) => e.preventDefault()}
        >
          {holding ? "Move your phone…" : "Hold & move phone"}
        </button>
        <button
          className="btn cursorclick"
          onPointerDown={() => {
            buzz();
            conn.current?.click();
          }}
        >
          Click
        </button>
      </div>
      {error && <div className="error">{error}</div>}
    </div>
  );
}
