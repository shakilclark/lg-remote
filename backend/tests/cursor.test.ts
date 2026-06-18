// D003 / US5 — the cursor WebSocket forwards move/click to the TV pointer socket.
import { afterEach, beforeEach, expect, test } from "vitest";
import { WebSocket } from "ws";
import { buildApp, type AppContext } from "../src/server.js";
import { MockTV } from "./mock-tv.js";
import { tempStore, lgtvFactoryForMock, waitFor } from "./helpers.js";

let mock: MockTV;
let ctx: AppContext;
let port: number;

beforeEach(async () => {
  mock = new MockTV();
  const mockUrl = await mock.start();
  ctx = buildApp({
    store: tempStore(),
    serveStatic: false,
    autoConnect: false,
    tvDeps: { createLgtv: lgtvFactoryForMock(mockUrl), discover: async () => [] },
  });
  port = await ctx.start(0);
  await fetch(`http://127.0.0.1:${port}/api/tvs`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ address: "10.0.0.5" }),
  });
  await waitFor(async () =>
    (await fetch(`http://127.0.0.1:${port}/api/state`).then((r) => r.json())).status === "connected",
  );
});

afterEach(async () => {
  await ctx.stop();
  await mock.stop();
});

test("move + click frames reach the TV pointer socket", async () => {
  const ws = new WebSocket(`ws://127.0.0.1:${port}/api/cursor`);
  await new Promise((res) => ws.on("open", res));
  ws.send(JSON.stringify({ type: "move", dx: 12, dy: -8 }));
  ws.send(JSON.stringify({ type: "move", dx: 3, dy: 4 }));
  ws.send(JSON.stringify({ type: "click" }));

  await waitFor(() => mock.moves.length >= 2 && mock.clicks >= 1);
  expect(mock.moves[0]).toEqual({ dx: 12, dy: -8 });
  expect(mock.clicks).toBe(1);
  ws.close();
});
