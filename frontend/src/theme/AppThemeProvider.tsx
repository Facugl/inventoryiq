import { useMemo, type ReactNode } from 'react'
import { ThemeProvider, CssBaseline, useMediaQuery } from '@mui/material'
import { createAppTheme } from './index'

/** Sigue prefers-color-scheme del SO, igual que index.css antes de esta migración. */
export function AppThemeProvider({ children }: { children: ReactNode }) {
  const prefersDark = useMediaQuery('(prefers-color-scheme: dark)')
  const theme = useMemo(() => createAppTheme(prefersDark ? 'dark' : 'light'), [prefersDark])

  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {children}
    </ThemeProvider>
  )
}
