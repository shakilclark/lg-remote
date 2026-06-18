// Minimal service worker — required for installability. Network-first so the app is always
// fresh online and still opens offline (shell fallback). /api/* is never intercepted.
const CACHE = "lg-remote-v1";
const SHELL = ["/", "/index.html"];

self.addEventListener("install", (e) => {
  self.skipWaiting();
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(SHELL)));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(
    (async () => {
      for (const k of await caches.keys()) if (k !== CACHE) await caches.delete(k);
      await self.clients.claim();
    })(),
  );
});

self.addEventListener("fetch", (e) => {
  const url = new URL(e.request.url);
  // Let the network/WS handle the API and anything non-GET.
  if (e.request.method !== "GET" || url.pathname.startsWith("/api")) return;
  e.respondWith(
    (async () => {
      try {
        const fresh = await fetch(e.request);
        const cache = await caches.open(CACHE);
        cache.put(e.request, fresh.clone());
        return fresh;
      } catch {
        const cached = await caches.match(e.request);
        return cached || (await caches.match("/index.html"));
      }
    })(),
  );
});
