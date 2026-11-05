import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  server: { proxy: { '/api/auth': { target: 'http://localhost:8080', changeOrigin: false } } },
  plugins: [react(), tailwindcss()],
})