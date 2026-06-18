// T011 — server boots and serves GET /api/state.
import { afterAll, beforeAll, expect, test } from "vitest";
import { buildApp, type AppContext } from "../src/server.js";
import { tempStore } from "./helpers.js";

let ctx: AppContext;
let base: string;

beforeAll(async () => {
  ctx = buildApp({ store: tempStore(), autoConnect: false, serveStatic: false });
  const port = await ctx.start(0);
  base = `http://127.0.0.1:${port}`;
});

afterAll(async () => {
  await ctx.stop();
});

test("GET /api/state returns the initial disconnected state", async () => {
  const res = await fetch(`${base}/api/state`);
  expect(res.status).toBe(200);
  const body = await res.json();
  expect(body.status).toBe("disconnected");
  expect(body.tvId).toBeNull();
});

test("GET /api/tvs is empty initially", async () => {
  const res = await fetch(`${base}/api/tvs`);
  const body = await res.json();
  expect(body.tvs).toEqual([]);
  expect(body.activeId).toBeNull();
});
