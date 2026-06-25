import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Proxy API calls during development. The assistant routes go to the
    // orchestrator microservice (8090); everything else to the backend (8080).
    // The more specific prefix must come first.
    proxy: {
      '/api/assistant': 'http://localhost:8090',
      '/api': 'http://localhost:8080',
    },
  },
})
