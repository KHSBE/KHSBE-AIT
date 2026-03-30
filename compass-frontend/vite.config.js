import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite' // ★ 새로 추가된 부분

export default defineConfig({
  plugins: [
    react(),
    tailwindcss(), // ★ 새로 추가된 부분
  ],
})