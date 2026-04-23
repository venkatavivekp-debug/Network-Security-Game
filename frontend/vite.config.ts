import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/auth": "http://localhost:8080",
      "/message": "http://localhost:8080",
      "/simulation": "http://localhost:8080",
      "/attack": "http://localhost:8080",
      "/logout": "http://localhost:8080",
    },
  },
});
