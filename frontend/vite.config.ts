import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Dev: proxy API + events WS to the backend. Build: emit into backend/public so the
// backend serves a single installable app on the Raspberry Pi.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        ws: true,
      },
    },
  },
  build: {
    outDir: "../backend/public",
    emptyOutDir: true,
  },
});
