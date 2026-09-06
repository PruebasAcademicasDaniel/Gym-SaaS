import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Sin proxy de /api a propósito: el cliente HTTP hace fetch absoluto a
// VITE_API_URL directamente desde el navegador. Un proxy acá correría
// dentro del proceso de Vite — que en docker compose vive en el
// contenedor del frontend, donde "localhost:8080" no llega al contenedor
// del backend (haría falta "http://backend:8080"). Pidiendo desde el
// navegador en cambio, siempre se resuelve contra el puerto publicado en
// el host, sin importar si Vite corre en Docker o local.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      '@': '/src',
    },
  },
  server: {
    port: 5173,
  },
})
