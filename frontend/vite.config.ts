import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';

// El servidor de desarrollo escucha en el 5173: es el origen que el backend autoriza por CORS
// (propiedad sga.cors.origenes-permitidos). strictPort evita que Vite use otro puerto en silencio,
// porque con otro origen el navegador bloquearía todas las llamadas a la API.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    // Sin VITE_API_URL el cliente llama a /api (y /imagenes): en desarrollo el proxy las reenvía al backend local
    proxy: {
      '/api': 'http://localhost:8080',
      '/imagenes': 'http://localhost:8080',
    },
  },
});
