// Shared domain types — mirrors specs/001-webos-remote/data-model.md

export type ConnectionStatus =
  | "needs-pairing"
  | "connecting"
  | "connected"
  | "disconnected"
  | "off-network";

/** A remembered television. Persisted (including the sensitive clientKey). */
export interface TVConnection {
  id: string;
  name: string;
  address: string;
  clientKey: string | null;
  lastConnectedAt: string | null;
}

/** What we expose about a TV to the frontend — never includes clientKey. */
export interface TVConnectionPublic {
  id: string;
  name: string;
  address: string;
  paired: boolean;
}

/** The backend's authoritative view of the link to the active TV. */
export interface ConnectionState {
  status: ConnectionStatus;
  tvId: string | null;
  volume?: number;
  muted?: boolean;
  message?: string;
}

export type NavButton =
  | "UP"
  | "DOWN"
  | "LEFT"
  | "RIGHT"
  | "ENTER"
  | "BACK"
  | "HOME"
  | "EXIT";

export type ControlCommandType =
  | "volumeUp"
  | "volumeDown"
  | "setMute"
  | "playPause"
  | "nav";

export interface ControlCommand {
  type: ControlCommandType;
  params?: { mute?: boolean; button?: NavButton };
}

export interface ControlResult {
  result: "acknowledged" | "failed";
  message?: string;
  state?: ConnectionState;
}

export function toPublic(tv: TVConnection): TVConnectionPublic {
  return {
    id: tv.id,
    name: tv.name,
    address: tv.address,
    paired: tv.clientKey != null,
  };
}
