// T031 — socket error codes map to off-network vs disconnected.
import { EventEmitter } from "node:events";
import { expect, test } from "vitest";
import { ConnectionStateMachine } from "../src/state/connection.js";
import { TVClient } from "../src/tv/client.js";
import { tempStore } from "./helpers.js";
import type { TVConnection } from "../src/types.js";

function fakeLgtv() {
  const e = new EventEmitter() as EventEmitter & {
    disconnect(): void;
    request(): void;
    subscribe(): void;
    getSocket(): void;
  };
  e.disconnect = () => {};
  e.request = () => {};
  e.subscribe = () => {};
  e.getSocket = () => {};
  return e;
}

const tv: TVConnection = {
  id: "addr:10.0.0.5",
  name: "TV",
  address: "10.0.0.5",
  clientKey: "k",
  lastConnectedAt: null,
};

test("EHOSTUNREACH → off-network; ECONNRESET → disconnected", () => {
  const store = tempStore();
  store.upsert(tv);
  const state = new ConnectionStateMachine();
  const fake = fakeLgtv();
  const client = new TVClient(store, state, {
    createLgtv: (() => fake) as never,
    discover: async () => [],
  });
  client.connectTo(tv);

  fake.emit("error", Object.assign(new Error("unreachable"), { code: "EHOSTUNREACH" }));
  expect(state.get().status).toBe("off-network");

  fake.emit("connect");
  expect(state.get().status).toBe("connected");

  fake.emit("error", Object.assign(new Error("reset"), { code: "ECONNRESET" }));
  expect(state.get().status).toBe("disconnected");
});
