// REST routes — contract: specs/001-webos-remote/contracts/backend-api.md
// Slice 1 (US1): tvs, discover, add TV, pair, state. Command routes arrive in slice 2.

import { Router, type Request, type Response, type NextFunction } from "express";
import { z } from "zod";
import type { Store } from "../store/store.js";
import type { ConnectionStateMachine } from "../state/connection.js";
import type { TVClient } from "../tv/client.js";
import type { Commands } from "../tv/commands.js";
import { toPublic, type ControlCommand, type TVConnection } from "../types.js";
import { discoverTVs } from "../tv/discovery.js";

export interface RestDeps {
  store: Store;
  state: ConnectionStateMachine;
  tvClient: TVClient;
  commands: Commands;
  discover?: (timeoutMs?: number) => Promise<Awaited<ReturnType<typeof discoverTVs>>>;
}

const addTvSchema = z.object({
  address: z.string().min(1).max(255),
  name: z.string().min(1).max(120).optional(),
});

const commandSchema = z
  .object({
    type: z.enum(["volumeUp", "volumeDown", "setMute", "playPause", "nav"]),
    params: z
      .object({
        mute: z.boolean().optional(),
        button: z
          .enum(["UP", "DOWN", "LEFT", "RIGHT", "ENTER", "BACK", "HOME", "EXIT"])
          .optional(),
      })
      .optional(),
  })
  .refine((c) => c.type !== "nav" || !!c.params?.button, {
    message: "nav requires params.button",
  });

export function createRestRouter(deps: RestDeps): Router {
  const { store, state, tvClient, commands } = deps;
  const discover = deps.discover ?? discoverTVs;
  const router = Router();

  router.get("/tvs", (_req, res) => {
    res.json({ tvs: store.list().map(toPublic), activeId: store.activeId });
  });

  router.post("/discover", asyncH(async (_req, res) => {
    const found = await discover();
    res.json({ found });
  }));

  router.post("/tvs", (req, res) => {
    const parsed = addTvSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(422).json({ message: "Invalid TV: address is required" });
    }
    const { address, name } = parsed.data;
    // Reuse an existing record for this address if present (keeps the pairing key).
    const existing = store.list().find((t) => t.address === address);
    const tv: TVConnection = existing ?? {
      id: `addr:${address}`,
      name: name ?? "LG TV",
      address,
      clientKey: null,
      lastConnectedAt: null,
    };
    if (name) tv.name = name;
    store.upsert(tv);
    tvClient.connectTo(tv);
    res.json({ id: tv.id, activeId: store.activeId });
  });

  router.post("/pair", (_req, res) => {
    tvClient.pairActive();
    const s = state.get();
    const message =
      s.status === "needs-pairing"
        ? "Accept the prompt on your TV"
        : s.message;
    res.json({ status: s.status, message });
  });

  router.get("/state", (_req, res) => {
    res.json(state.get());
  });

  router.post("/command", asyncH(async (req, res) => {
    const parsed = commandSchema.safeParse(req.body);
    if (!parsed.success) {
      return res.status(422).json({ result: "failed", message: "Unknown command" });
    }
    const result = await commands.send(parsed.data as ControlCommand);
    if (result.result === "acknowledged") return res.json(result);
    // Distinguish "can't right now" (connection) from other failures.
    const status = /not connected|off-network/i.test(result.message ?? "") ? 409 : 502;
    return res.status(status).json(result);
  }));

  return router;
}

type AsyncHandler = (req: Request, res: Response) => Promise<unknown>;
function asyncH(fn: AsyncHandler) {
  return (req: Request, res: Response, next: NextFunction) => {
    fn(req, res).catch(next);
  };
}
