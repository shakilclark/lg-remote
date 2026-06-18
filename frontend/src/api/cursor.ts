// WebSocket client for the low-latency motion cursor (US5). Queues frames until open.

export interface CursorConn {
  move(dx: number, dy: number): void;
  click(): void;
  close(): void;
}

export function openCursor(): CursorConn {
  const proto = location.protocol === "https:" ? "wss" : "ws";
  const ws = new WebSocket(`${proto}://${location.host}/api/cursor`);
  const queue: string[] = [];
  let open = false;

  ws.onopen = () => {
    open = true;
    for (const m of queue) ws.send(m);
    queue.length = 0;
  };
  ws.onerror = () => {};

  const send = (o: unknown) => {
    const s = JSON.stringify(o);
    if (open && ws.readyState === WebSocket.OPEN) ws.send(s);
    else queue.push(s);
  };

  return {
    move: (dx, dy) => send({ type: "move", dx, dy }),
    click: () => send({ type: "click" }),
    close: () => ws.close(),
  };
}
