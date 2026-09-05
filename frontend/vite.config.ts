import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// http://localhost:8080 sirve para desarrollo local (backend corriendo aparte).
// docker-compose.yml sobreescribe esto con VITE_BACKEND_URL=http://backend:8080
// (nombre del servicio en la red de Docker) para el contenedor del frontend.
const backendUrl = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backendUrl,
      '/health': backendUrl,
    },
  },
})
