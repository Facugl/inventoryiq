import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// http://localhost:8080 sirve para desarrollo local (backend corriendo aparte).
// docker-compose.yml sobreescribe esto con VITE_BACKEND_URL=http://backend:8080
// (nombre del servicio en la red de Docker) para el contenedor del frontend.
const backendUrl = process.env.VITE_BACKEND_URL ?? 'http://localhost:8080'

// Dentro de Docker Desktop (Windows/Mac), los eventos de filesystem del bind
// mount no llegan al contenedor: sin polling, Vite no detecta cambios de
// archivo y HMR queda roto. VITE_BACKEND_URL solo se define en
// docker-compose.yml, así que sirve también como señal de "corriendo en Docker".
const runningInDocker = Boolean(process.env.VITE_BACKEND_URL)

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': backendUrl,
      '/health': backendUrl,
    },
    watch: runningInDocker ? { usePolling: true, interval: 300 } : undefined,
    // Por default Vite solo acepta Host: localhost (protección contra DNS
    // rebinding) — sin esto, el dev server rechaza cualquier dominio público
    // como el de Render con "Blocked request".
    allowedHosts: ['.onrender.com'],
  },
})
