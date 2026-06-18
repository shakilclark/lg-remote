import { useEffect, useState } from "react";
import { api, subscribeState, type ConnectionState } from "../api/client";

const INITIAL: ConnectionState = { status: "connecting", tvId: null };

const UNREACHABLE: ConnectionState = {
  status: "off-network",
  tvId: null,
  message: "Can't reach the remote — check you're on the same network or Tailscale.",
};

/** Live connection state from the backend. When the backend itself is unreachable
 *  (phone off-network), reports off-network rather than a stale last-known state. */
export function useConnection(): ConnectionState {
  const [state, setState] = useState<ConnectionState>(INITIAL);
  const [reachable, setReachable] = useState(true);

  useEffect(() => {
    let active = true;
    api.getState().then((s) => active && setState(s)).catch(() => active && setReachable(false));
    const unsub = subscribeState({
      onState: (s) => active && setState(s),
      onReachable: (r) => active && setReachable(r),
    });
    return () => {
      active = false;
      unsub();
    };
  }, []);

  return reachable ? state : UNREACHABLE;
}
