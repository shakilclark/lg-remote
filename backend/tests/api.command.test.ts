// T020 — POST /api/command: control actions acknowledged; rejects when disconnected / invalid.
import { afterEach, beforeEach, expect, test } from "vitest";
import { buildApp, type AppContext } from "../src/server.js";
import { MockTV } from "./mock-tv.js";
import { tempStore, lgtvFactoryForMock, waitFor } from "./helpers.js";

let mock: MockTV;
let ctx: AppContext;
let base: string;

async function connectedApp() {
  mock = new MockTV();
  const mockUrl = await mock.start();
  ctx = buildApp({
    store: tempStore(),
    serveStatic: false,
    autoConnect: false,
    tvDeps: { createLgtv: lgtvFactoryForMock(mockUrl), discover: async () => [] },
  });
  const port = await ctx.start(0);
  base = `http://127.0.0.1:${port}`;
}

const j = (r: Response) => r.json();
const cmd = (body: unknown) =>
  fetch(`${base}/api/command`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify(body),
  });

afterEach(async () => {
  await ctx.stop();
  await mock?.stop();
});

test("volume / mute / play-pause are acknowledged when connected", async () => {
  await connectedApp();
  await fetch(`${base}/api/tvs`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ address: "10.0.0.5" }),
  });
  await waitFor(async () => (await fetch(`${base}/api/state`).then(j)).status === "connected");

  // Volume subscription should reflect the TV's reported level (nested volumeStatus shape).
  await waitFor(async () => (await fetch(`${base}/api/state`).then(j)).volume === 13);

  for (const body of [
    { type: "volumeUp" },
    { type: "volumeDown" },
    { type: "playPause" },
  ]) {
    const res = await cmd(body);
    expect(res.status).toBe(200);
    expect((await res.json()).result).toBe("acknowledged");
  }

  const muteRes = await cmd({ type: "setMute", params: { mute: true } }).then(j);
  expect(muteRes.result).toBe("acknowledged");
  expect(muteRes.state.muted).toBe(true);
});

test("unknown command type → 422", async () => {
  await connectedApp();
  const res = await cmd({ type: "selfDestruct" });
  expect(res.status).toBe(422);
});

test("command while disconnected → 409", async () => {
  await connectedApp(); // built, but no TV added → not connected
  const res = await cmd({ type: "volumeUp" });
  expect(res.status).toBe(409);
  expect((await res.json()).result).toBe("failed");
});
