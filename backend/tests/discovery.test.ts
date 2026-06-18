// Regression: a webOS TV answers SSDP for several services (each its own UDN); the list
// must collapse to ONE entry per address. (Bug found in slice-1 demo: TV shown 4 times.)
import { expect, test } from "vitest";
import { dedupeByAddress } from "../src/tv/discovery.js";

test("collapses many UPnP services from one TV into a single entry", () => {
  const candidates = [
    { address: "192.168.0.50", name: "[LG] webOS TV", udn: "uuid-b" },
    { address: "192.168.0.50", name: "LG Smart TV", udn: "uuid-a" },
    { address: "192.168.0.50", name: "DMR", udn: "uuid-c" },
    { address: "192.168.0.50", name: "[LG] webOS TV", udn: "uuid-b" },
  ];
  const out = dedupeByAddress(candidates);
  expect(out).toHaveLength(1);
  expect(out[0].address).toBe("192.168.0.50");
  // Deterministic id = smallest UDN; name prefers a TV-ish label.
  expect(out[0].id).toBe("uuid-a");
  expect(out[0].name).toMatch(/webos|tv|lg/i);
});

test("keeps distinct TVs at different addresses, sorted by address", () => {
  const out = dedupeByAddress([
    { address: "192.168.0.51", name: "Bedroom", udn: null },
    { address: "192.168.0.50", name: "Living Room", udn: "uuid-x" },
  ]);
  expect(out.map((t) => t.address)).toEqual(["192.168.0.50", "192.168.0.51"]);
  // No UDN -> address-derived id.
  expect(out[1].id).toBe("addr:192.168.0.51");
});
