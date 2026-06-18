// D004 / US6 — app shortcuts launch the right app id (resolved from launch points).
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
  // Wait for launch points to be cached after connect.
  await waitFor(() => mock.lastRequests.some((u) => u.includes("listLaunchPoints")));
});

afterEach(async () => {
  await ctx.stop();
  await mock.stop();
});

const launch = (app: string) =>
  fetch(`${base}/api/command`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ type: "launchApp", params: { app } }),
  });

test("YouTube and Netflix shortcuts launch the resolved app ids", async () => {
  expect((await launch("youtube")).status).toBe(200);
  expect((await launch("netflix")).status).toBe(200);
  await waitFor(() => mock.launched.length >= 2);
  expect(mock.launched).toContain("youtube.leanback.v4");
  expect(mock.launched).toContain("netflix");
});

test("launchApp without an app → 422", async () => {
  const res = await fetch(`${base}/api/command`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ type: "launchApp" }),
  });
  expect(res.status).toBe(422);
});
