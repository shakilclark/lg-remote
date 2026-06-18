import { useEffect, useState } from "react";
import { api, subscribeState, type ConnectionState } from "../api/client";

const INITIAL: ConnectionState = { status: "connecting", tvId: null };

/** Live connection state from the backend (snapshot on connect + pushes thereafter). */
export function useConnection(): ConnectionState {
  const [state, setState] = useState<ConnectionState>(INITIAL);

  useEffect(() => {
    let active = true;
    // Seed with a one-shot fetch so we render truth even before the WS opens.
    api.getState().then((s) => active && setState(s)).catch(() => {});
    const unsub = subscribeState((s) => active && setState(s));
    return () => {
      active = false;
      unsub();
    };
  }, []);

  return state;
}
