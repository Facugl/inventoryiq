import { createTheme, type PaletteMode } from '@mui/material/styles'
import { lightPalette, darkPalette } from './palette'

export function createAppTheme(mode: PaletteMode) {
  return createTheme({
    palette: mode === 'dark' ? darkPalette : lightPalette,
    typography: {
      fontFamily: 'system-ui, "Segoe UI", Roboto, sans-serif',
    },
    shape: {
      borderRadius: 6,
    },
    components: {
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: { textTransform: 'none' },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          head: { fontWeight: 500 },
        },
      },
    },
  })
}
