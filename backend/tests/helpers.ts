import { tmpdir } from "node:os";
import { join } from "node:path";
import lgtv from "lgtv2";
import { Store } from "../src/store/store.js";

let seq = 0;

/** A fresh Store backed by a throwaway temp file (never touches ~/.config). */
export function tempStore(): Store {
  const path = join(tmpdir(), `lg-remote-test-${process.pid}-${seq++}-${Date.now()}.json`);
  return new Store(path);
}

/**
 * A createLgtv factory that points real lgtv2 at the mock server URL regardless of the
 * address the app computes, and disables the reconnect loop so tests leave no timers.
 */
export function lgtvFactoryForMock(mockUrl: string): typeof lgtv {
  return ((opts: Parameters<typeof lgtv>[0] = {}) =>
    lgtv({
      ...opts,
      url: mockUrl,
      reconnect: false,
      // Unique throwaway key-file per instance so tests never read a shared/stale key.
      keyFile: join(tmpdir(), `lg-remote-key-${process.pid}-${seq++}-${Date.now()}`),
    })) as typeof lgtv;
}

export async function waitFor(
  predicate: () => boolean | Promise<boolean>,
  { timeout = 4000, interval = 25 } = {},
): Promise<void> {
  const start = Date.now();
  while (!(await predicate())) {
    if (Date.now() - start > timeout) throw new Error("waitFor: timed out");
    await new Promise((r) => setTimeout(r, interval));
  }
}
