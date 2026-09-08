import type { PaletteOptions } from '@mui/material/styles'

/**
 * Mismos valores que index.css (:root y @media prefers-color-scheme: dark),
 * para que la migración a MUI no cambie la identidad visual ya validada del
 * proyecto — fondo oscuro/claro con acento violeta.
 */
export const lightPalette: PaletteOptions = {
  mode: 'light',
  primary: { main: '#aa3bff' },
  background: { default: '#ffffff', paper: '#f4f3ec' },
  text: { primary: '#08060d', secondary: '#6b6375' },
  divider: '#e5e4e7',
}

export const darkPalette: PaletteOptions = {
  mode: 'dark',
  primary: { main: '#c084fc' },
  background: { default: '#16171d', paper: '#1f2028' },
  text: { primary: '#f3f4f6', secondary: '#9ca3af' },
  divider: '#2e303a',
}
