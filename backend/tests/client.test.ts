// T013 — TVClient maps lgtv2 events (prompt/connect) to ConnectionState and persists the key.
import { afterEach, beforeEach, expect, test } from "vitest";
import { ConnectionStateMachine } from "../src/state/connection.js";
import { TVClient } from "../src/tv/client.js";
import { MockTV } from "./mock-tv.js";
import { tempStore, lgtvFactoryForMock, waitFor } from "./helpers.js";
import type { TVConnection } from "../src/types.js";

let mock: MockTV;
let mockUrl: string;

beforeEach(async () => {
  mock = new MockTV();
  mockUrl = await mock.start();
});

afterEach(async () => {
  await mock.stop();
});

const tv: TVConnection = {
  id: "addr:10.0.0.5",
  name: "Test TV",
  address: "10.0.0.5",
  clientKey: null,
  lastConnectedAt: null,
};

test("first pairing: connecting → needs-pairing → connected, key persisted", async () => {
  const store = tempStore();
  store.upsert(tv);
  const state = new ConnectionStateMachine();
  const seen: string[] = [];
  state.on("change", (s) => seen.push(s.status));

  const client = new TVClient(store, state, {
    createLgtv: lgtvFactoryForMock(mockUrl),
    discover: async () => [],
  });
  client.connectTo(tv);

  await waitFor(() => state.get().status === "connected");

  expect(seen).toContain("needs-pairing");
  expect(seen).toContain("connected");
  expect(store.get(tv.id)?.clientKey).toBe(mock.clientKey);
  expect(store.activeId).toBe(tv.id);

  client.disconnect();
  expect(state.get().status).toBe("disconnected");
});

test("already paired: connects without a prompt", async () => {
  const store = tempStore();
  store.upsert({ ...tv, clientKey: "MOCK-CLIENT-KEY" });
  const state = new ConnectionStateMachine();
  const seen: string[] = [];
  state.on("change", (s) => seen.push(s.status));

  const client = new TVClient(store, state, {
    createLgtv: lgtvFactoryForMock(mockUrl),
    discover: async () => [],
  });
  client.connectTo({ ...tv, clientKey: "MOCK-CLIENT-KEY" });

  await waitFor(() => state.get().status === "connected");
  expect(seen).not.toContain("needs-pairing");
  expect(mock.promptCount).toBe(0);

  client.disconnect();
});
