import { api, buzz } from "../api/client";

export function VolumePad({ volume, muted }: { volume?: number; muted?: boolean }) {
  const vol = (dir: "volumeUp" | "volumeDown") => {
    buzz();
    api.command(dir).catch(() => {});
  };
  const toggleMute = () => {
    buzz();
    api.command("setMute", { mute: !muted }).catch(() => {});
  };

  return (
    <div className="volume">
      <button className="btn vbtn" onPointerDown={() => vol("volumeUp")} aria-label="Volume up">
        <span className="ico">＋</span>
      </button>
      <div className="vlevel">
        Volume <b>{volume ?? "—"}</b>
      </div>
      <button className="btn vbtn" onPointerDown={() => vol("volumeDown")} aria-label="Volume down">
        <span className="ico">－</span>
      </button>
      <button
        className={`btn mute ${muted ? "on" : ""}`}
        onPointerDown={toggleMute}
        aria-label={muted ? "Unmute" : "Mute"}
      >
        <span className="ico">{muted ? "🔇" : "🔈"}</span>
        <span className="lbl">{muted ? "Muted" : "Mute"}</span>
      </button>
    </div>
  );
}
