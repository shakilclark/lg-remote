// T012 — REST pairing flow: discover, add TV, pair → connected; key persisted; never leaks key.
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
    discover: async () => [
      { id: "addr:10.0.0.5", name: "Living Room TV", address: "10.0.0.5" },
    ],
  });
  const port = await ctx.start(0);
  base = `http://127.0.0.1:${port}`;
});

afterEach(async () => {
  await ctx.stop();
  await mock.stop();
});

const j = (res: Response) => res.json();

test("POST /api/discover returns TVs found on the network", async () => {
  const found = await fetch(`${base}/api/discover`, { method: "POST" }).then(j);
  expect(found.found).toHaveLength(1);
  expect(found.found[0].address).toBe("10.0.0.5");
});

test("add TV + pair reaches connected and persists the key (never exposed)", async () => {
  const add = await fetch(`${base}/api/tvs`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ address: "10.0.0.5", name: "Living Room TV" }),
  }).then(j);
  expect(add.id).toBe("addr:10.0.0.5");

  await waitFor(async () => (await fetch(`${base}/api/state`).then(j)).status === "connected");

  const tvs = await fetch(`${base}/api/tvs`).then(j);
  expect(tvs.tvs[0].paired).toBe(true);
  // The pairing key must never appear in any API response.
  expect(JSON.stringify(tvs)).not.toContain("MOCK-CLIENT-KEY");
});

test("POST /api/tvs with no address is rejected (422)", async () => {
  const res = await fetch(`${base}/api/tvs`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({}),
  });
  expect(res.status).toBe(422);
});
