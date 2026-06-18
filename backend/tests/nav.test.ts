// T025 — nav commands acquire the pointer-input socket once and send the right buttons.
import { afterEach, beforeEach, expect, test } from "vitest";
import { buildApp, type AppContext } from "../src/server.js";
import { MockTV } from "./mock-tv.js";
import { tempStore, lgtvFactoryForMock, waitFor } from "./helpers.js";

let mock: MockTV;
let ctx: AppContext;
let base: string;

beforeEach(async () => {
  mock = new MockTV();
  const mockUrl = await mock.start();
  ctx = buildApp({
    store: tempStore(),
    serveStatic: false,
    autoConnect: false,
    tvDeps: { createLgtv: lgtvFactoryForMock(mockUrl), discover: async () => [] },
  });
  base = `http://127.0.0.1:${await ctx.start(0)}`;
  await fetch(`${base}/api/tvs`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ address: "10.0.0.5" }),
  });
  await waitFor(async () => (await fetch(`${base}/api/state`).then((r) => r.json())).status === "connected");
});

afterEach(async () => {
  await ctx.stop();
  await mock.stop();
});

const nav = (button: string) =>
  fetch(`${base}/api/command`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ type: "nav", params: { button } }),
  });

test("d-pad buttons reach the TV; pointer socket acquired once", async () => {
  for (const b of ["UP", "DOWN", "ENTER", "BACK", "HOME"]) {
    const res = await nav(b);
    expect(res.status).toBe(200);
  }
  await waitFor(() => mock.buttons.length >= 5);
  expect(mock.buttons).toEqual(["UP", "DOWN", "ENTER", "BACK", "HOME"]);
  // getPointerInputSocket requested exactly once despite 5 presses.
  const acquisitions = mock.lastRequests.filter((u) => u.includes("getPointerInputSocket"));
  expect(acquisitions).toHaveLength(1);
});

test("invalid button → 422", async () => {
  const res = await nav("DIAGONAL");
  expect(res.status).toBe(422);
});
