import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Proxy /api sang backend Spring Boot (dev) để tránh CORS
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
