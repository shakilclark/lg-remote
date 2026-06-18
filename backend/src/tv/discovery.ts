// SSDP discovery of LG webOS TVs on the LAN. Manual IP entry is the fallback (handled in REST).

import ssdp, { type SsdpHeaders } from "node-ssdp";

const { Client } = ssdp;

export interface DiscoveredTV {
  id: string; // UPnP UDN when available, else derived from address
  name: string;
  address: string; // host/IP only
}

const WEBOS_SERVICE = "urn:lge-com:service:webos-second-screen:1";

/** Run a short SSDP scan and return de-duplicated LG TVs found. */
export async function discoverTVs(timeoutMs = 4000): Promise<DiscoveredTV[]> {
  const client = new Client();
  const byId = new Map<string, DiscoveredTV>();
  const seenLocations = new Set<string>();
  const pending: Promise<void>[] = [];

  client.on("response", (headers: SsdpHeaders) => {
    const location = headers.LOCATION;
    const server = headers.SERVER ?? "";
    if (!location || seenLocations.has(location)) return;
    seenLocations.add(location);
    // Fast filter: webOS TVs advertise an LG/webOS server string or the webOS service.
    const looksLG = /webos|lg /i.test(server) || /webos/i.test(headers.ST ?? "");
    pending.push(
      resolveDevice(location, looksLG).then((tv) => {
        if (tv) byId.set(tv.id, tv);
      }),
    );
  });

  client.search(WEBOS_SERVICE);
  client.search("ssdp:all");

  await delay(timeoutMs);
  client.stop();
  await Promise.allSettled(pending);
  return [...byId.values()];
}

async function resolveDevice(
  location: string,
  preFiltered: boolean,
): Promise<DiscoveredTV | null> {
  try {
    const host = new URL(location).hostname;
    const res = await fetch(location, { signal: AbortSignal.timeout(2500) });
    const xml = await res.text();
    const isLG = /lg electronics|webos/i.test(xml);
    if (!preFiltered && !isLG) return null;
    const name = match(xml, /<friendlyName>([^<]+)<\/friendlyName>/i) ?? "LG TV";
    const udn = match(xml, /<UDN>(?:uuid:)?([^<]+)<\/UDN>/i);
    return { id: udn ?? `addr:${host}`, name: name.trim(), address: host };
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
