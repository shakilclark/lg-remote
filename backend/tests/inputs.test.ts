// D005 / US7 — list inputs and switch to one.
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

const j = (r: Response) => r.json();

test("GET /api/inputs lists the TV's external inputs", async () => {
  const { inputs } = await fetch(`${base}/api/inputs`).then(j);
  expect(inputs).toEqual([
    { id: "HDMI_1", label: "HDMI 1" },
    { id: "HDMI_2", label: "Xbox" },
  ]);
});

test("setInput switches to the chosen source", async () => {
  const res = await fetch(`${base}/api/command`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ type: "setInput", params: { inputId: "HDMI_2" } }),
  });
  expect(res.status).toBe(200);
  await waitFor(() => mock.switchedTo.includes("HDMI_2"));
});

test("setInput without inputId → 422", async () => {
  const res = await fetch(`${base}/api/command`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ type: "setInput" }),
  });
  expect(res.status).toBe(422);
});
