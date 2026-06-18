import { api, buzz, type CommandParams } from "../api/client";

type Button = NonNullable<CommandParams["button"]>;

function press(button: Button) {
  buzz();
  api.command("nav", { button }).catch(() => {});
}

export function DPad() {
  return (
    <>
      <div className="pad">
        <span className="corner" />
        <button className="btn up" onPointerDown={() => press("UP")} aria-label="Up">▲</button>
        <span className="corner" />
        <button className="btn left" onPointerDown={() => press("LEFT")} aria-label="Left">◀</button>
        <button className="btn ok" onPointerDown={() => press("ENTER")} aria-label="OK">OK</button>
        <button className="btn right" onPointerDown={() => press("RIGHT")} aria-label="Right">▶</button>
        <span className="corner" />
        <button className="btn down" onPointerDown={() => press("DOWN")} aria-label="Down">▼</button>
        <span className="corner" />
      </div>
      <div className="navrow">
        <button className="btn" onPointerDown={() => press("BACK")}>
          <span className="ico">↩</span><span className="lbl">Back</span>
        </button>
        <button className="btn" onPointerDown={() => press("HOME")}>
          <span className="ico">⌂</span><span className="lbl">Home</span>
        </button>
        <button className="btn" onPointerDown={() => press("EXIT")}>
          <span className="ico">✕</span><span className="lbl">Exit</span>
        </button>
      </div>
    </>
  );
}
