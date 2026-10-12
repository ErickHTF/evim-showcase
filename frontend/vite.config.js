import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: true,
    proxy: {
      '/deploy-api': {
        target: 'http://host.docker.internal:8081',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/deploy-api/, ''),
      },
    },
  },
})