import { useState } from 'react'
import { HomePage } from './features/home/HomePage'
import { CriticalProductsPage } from './features/critical-products/CriticalProductsPage'
import { OverstockPage } from './features/overstock/OverstockPage'
import './App.css'

const SCREENS = {
  home: { label: 'Inicio', render: () => <HomePage /> },
  critical: { label: 'Productos críticos', render: () => <CriticalProductsPage /> },
  overstock: { label: 'Sobrestock', render: () => <OverstockPage /> },
} as const

type ScreenKey = keyof typeof SCREENS

function App() {
  const [screen, setScreen] = useState<ScreenKey>('home')

  return (
    <>
      <nav className="app-nav">
        {(Object.keys(SCREENS) as ScreenKey[]).map((key) => (
          <button
            key={key}
            type="button"
            className={key === screen ? 'active' : ''}
            onClick={() => setScreen(key)}
          >
            {SCREENS[key].label}
          </button>
        ))}
      </nav>
      {SCREENS[screen].render()}
    </>
  )
}

export default App
