// SSDP discovery of LG webOS TVs on the LAN. Manual IP entry is the fallback (handled in REST).
// A webOS TV answers SSDP for several UPnP services, each with its own LOCATION + UDN — so we
// collect all candidates and collapse them to ONE entry per address (see dedupeByAddress).

import ssdp, { type SsdpHeaders } from "node-ssdp";

const { Client } = ssdp;

export interface DiscoveredTV {
  id: string; // UPnP UDN when available, else derived from address
  name: string;
  address: string; // host/IP only
}

interface Candidate {
  address: string;
  name: string;
  udn: string | null;
}

const WEBOS_SERVICE = "urn:lge-com:service:webos-second-screen:1";

/** Run a short SSDP scan and return one entry per LG TV found. */
export async function discoverTVs(timeoutMs = 4000): Promise<DiscoveredTV[]> {
  const client = new Client();
  const candidates: Candidate[] = [];
  const seenLocations = new Set<string>();
  const pending: Promise<void>[] = [];

  client.on("response", (headers: SsdpHeaders) => {
    const location = headers.LOCATION;
    const server = headers.SERVER ?? "";
    if (!location || seenLocations.has(location)) return;
    seenLocations.add(location);
    const looksLG = /webos|lg /i.test(server) || /webos/i.test(headers.ST ?? "");
    pending.push(
      resolveDevice(location, looksLG).then((c) => {
        if (c) candidates.push(c);
      }),
    );
  });

  client.search(WEBOS_SERVICE);
  client.search("ssdp:all");

  await delay(timeoutMs);
  client.stop();
  await Promise.allSettled(pending);
  return dedupeByAddress(candidates);
}

/** Collapse many UPnP service responses into one TV per address. Pure + deterministic. */
export function dedupeByAddress(candidates: Candidate[]): DiscoveredTV[] {
  const byAddress = new Map<string, Candidate[]>();
  for (const c of candidates) {
    const group = byAddress.get(c.address) ?? [];
    group.push(c);
    byAddress.set(c.address, group);
  }

  const result: DiscoveredTV[] = [];
  for (const [address, group] of byAddress) {
    // Deterministic id: smallest UDN seen for this address, else address-derived.
    const udns = group
      .map((c) => c.udn)
      .filter((u): u is string => !!u)
      .sort();
    const id = udns[0] ?? `addr:${address}`;
    // Prefer a name that looks like a TV; otherwise the first non-empty; else a default.
    const names = group.map((c) => c.name).filter(Boolean).sort();
    const name = names.find((n) => /webos|\btv\b|lg/i.test(n)) ?? names[0] ?? "LG TV";
    result.push({ id, name, address });
  }
  return result.sort((a, b) => a.address.localeCompare(b.address));
}

async function resolveDevice(
  location: string,
  preFiltered: boolean,
): Promise<Candidate | null> {
  try {
    const host = new URL(location).hostname;
    const res = await fetch(location, { signal: AbortSignal.timeout(2500) });
    const xml = await res.text();
    const isLG = /lg electronics|webos/i.test(xml);
    if (!preFiltered && !isLG) return null;
    const name = match(xml, /<friendlyName>([^<]+)<\/friendlyName>/i) ?? "LG TV";
    const udn = match(xml, /<UDN>(?:uuid:)?([^<]+)<\/UDN>/i);
    return { address: host, name: name.trim(), udn: udn?.trim() ?? null };
  } catch {
    return null;
  }
}

function match(s: string, re: RegExp): string | null {
  const m = s.match(re);
  return m ? m[1] : null;
}

function delay(ms: number): Promise<void> {
  return new Promise((r) => setTimeout(r, ms));
}
