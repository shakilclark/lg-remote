import { useEffect, useState } from "react";
import { api, buzz, type TVInput } from "../api/client";

export function InputSwitcher() {
  const [inputs, setInputs] = useState<TVInput[]>([]);

  useEffect(() => {
    let active = true;
    api.getInputs().then(({ inputs }) => active && setInputs(inputs)).catch(() => {});
    return () => {
      active = false;
    };
  }, []);

  if (inputs.length === 0) return null;

  const select = (id: string) => {
    buzz();
    api.command("setInput", { inputId: id }).catch(() => {});
  };

  return (
    <div className="inputs">
      <div className="cardlabel">Inputs</div>
      <div className="inputrow">
        {inputs.map((i) => (
          <button key={i.id} className="btn input" onPointerDown={() => select(i.id)}>
            {i.label}
          </button>
        ))}
      </div>
    </div>
  );
}
