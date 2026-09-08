import { QueryClient } from '@tanstack/react-query'

/**
 * Cada pantalla dispara su propia búsqueda con un botón "Buscar" — no hay
 * background sync que valga la pena, así que se desactiva el refetch
 * automático al volver a la pestaña y los reintentos silenciosos.
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: false,
    },
  },
})
