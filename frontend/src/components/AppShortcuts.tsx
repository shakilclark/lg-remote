import { api, buzz } from "../api/client";

function launch(app: "youtube" | "netflix") {
  buzz();
  api.command("launchApp", { app }).catch(() => {});
}

export function AppShortcuts() {
  return (
    <div className="shortcuts">
      <button className="btn shortcut yt" onPointerDown={() => launch("youtube")}>
        <span className="ico">▶</span>
        <span className="lbl">YouTube</span>
      </button>
      <button className="btn shortcut nf" onPointerDown={() => launch("netflix")}>
        <span className="ico">N</span>
        <span className="lbl">Netflix</span>
      </button>
    </div>
  );
}
