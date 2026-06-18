# HTTPS + away-from-home via Tailscale

A full-screen **installable** PWA needs a trusted HTTPS origin (Android won't install a PWA
served over plain `http://<lan-ip>`; a service worker requires a secure context). Tailscale
gives the host a real Let's Encrypt cert with no certificate fiddling, and as a bonus the
remote then works from outside the home — all over your own private overlay, no third-party
control cloud.

## One-time setup (on the host: Raspberry Pi, or this Mac for testing)

1. Install Tailscale and sign in:
   ```bash
   # macOS
   brew install tailscale && sudo tailscale up
   # Raspberry Pi / Linux
   curl -fsSL https://tailscale.com/install.sh | sh && sudo tailscale up
   ```
2. Enable HTTPS for your tailnet once (Tailscale admin console → DNS → **Enable HTTPS**).
3. Find the host's MagicDNS name:
   ```bash
   tailscale status        # e.g. raspberrypi.tailXXXX.ts.net
   ```

## Serve the app over HTTPS

With the backend running on `:8080`:

```bash
tailscale serve --bg 8080
```

This publishes it at `https://<host>.<tailnet>.ts.net` (proxying to `localhost:8080`,
including the `/api/events` WebSocket). Check it:

```bash
tailscale serve status
```

## Install on phone / iPad

1. Install Tailscale on the **Android phone** and **iPad**, sign into the same tailnet.
2. Open `https://<host>.<tailnet>.ts.net`.
3. **Android Chrome**: menu → **Install app** → launches full-screen.
   **iPad Safari**: Share → **Add to Home Screen** → launches without browser chrome.

Now it works on the home Wi-Fi *and* away (mobile data), because Tailscale links your
devices directly to the host. The host must remain on the same LAN as the TV.

## Notes
- Control traffic path: phone → (Tailscale, encrypted) → host → `ws://` → TV. Tailscale's
  coordination plane sees connection metadata only, never the SSAP/control traffic.
- Plain-LAN fallback (no Tailscale): open `http://<host-ip>:8080`. Works on the home network;
  iPad can still "Add to Home Screen" (standalone), but Android won't offer a true install.
