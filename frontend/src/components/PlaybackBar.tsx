import { api, buzz } from "../api/client";

export function PlaybackBar() {
  const playpause = () => {
    buzz();
    api.command("playPause").catch(() => {});
  };
  return (
    <button className="btn playpause" onPointerDown={playpause} aria-label="Play or pause">
      <span className="ico">⏯</span>
      <span className="lbl">Play / Pause</span>
    </button>
  );
}
