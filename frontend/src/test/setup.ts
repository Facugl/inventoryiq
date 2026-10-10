import '@testing-library/jest-dom/vitest'
import { afterEach } from 'vitest'
import { cleanup } from '@testing-library/react'

// Testing Library no desmonta solo cuando `globals: true` sin su plugin de
// auto-cleanup: sin esto, cada test vería el DOM del anterior.
afterEach(() => {
  cleanup()
})
