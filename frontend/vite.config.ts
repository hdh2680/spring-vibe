import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ command }) => ({
  plugins: [react()],
  // Dev server: keep base="/" so absolute URLs like "/css/..." stay absolute (not rewritten to "/app/css/...").
  // Build: emit assets assuming the app is hosted under "/app/" by Spring static hosting.
  base: command === "serve" ? "/" : "/app/",
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      // Spring Boot API
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      // Reuse Spring static assets (shared CSS/images) during dev.
      "/css": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/images": {
        target: "http://localhost:8080",
        changeOrigin: true
      },
      "/js": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
}));
