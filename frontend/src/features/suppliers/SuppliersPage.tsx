import { useEffect, useState } from 'react'
import { getSuppliers, updateSupplierLeadTime } from '../../api/suppliers'
import { ApiError } from '../../api/http'
import type { Supplier } from '../../api/types'
import '../../shared/list-page.css'
import './SuppliersPage.css'

function genericErrorMessage(err: unknown): string {
  return err instanceof ApiError ? err.message : 'No se pudo conectar con el servidor.'
}

export function SuppliersPage() {
  const [suppliers, setSuppliers] = useState<Supplier[] | null>(null)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [editingId, setEditingId] = useState<number | null>(null)
  const [draftLeadTime, setDraftLeadTime] = useState('')
  const [saving, setSaving] = useState(false)
  const [rowError, setRowError] = useState<string | null>(null)

  // Carga inicial. Todo setState ocurre dentro de then/catch (fuera del cuerpo
  // síncrono del efecto) para no disparar el warning de set-state-in-effect.
  useEffect(() => {
    let cancelled = false

    getSuppliers()
      .then((result) => {
        if (!cancelled) setSuppliers(result)
      })
      .catch((err) => {
        if (!cancelled) setLoadError(genericErrorMessage(err))
      })

    return () => {
      cancelled = true
    }
  }, [])

  const startEditing = (supplier: Supplier) => {
    setEditingId(supplier.supplierId)
    setDraftLeadTime(String(supplier.leadTimeDays))
    setRowError(null)
  }

  const cancelEditing = () => {
    setEditingId(null)
    setRowError(null)
  }

  const saveLeadTime = (supplierId: number) => {
    const parsed = Number(draftLeadTime)
    if (draftLeadTime === '' || !Number.isInteger(parsed) || parsed <= 0) {
      setRowError('Ingresá un número entero mayor a 0.')
      return
    }

    setSaving(true)
    setRowError(null)
    updateSupplierLeadTime(supplierId, parsed)
      .then((updated) => {
        setSuppliers((current) =>
          (current ?? []).map((supplier) => (supplier.supplierId === supplierId ? updated : supplier)),
        )
        setEditingId(null)
      })
      .catch((err) => setRowError(genericErrorMessage(err)))
      .finally(() => setSaving(false))
  }

  return (
    <main className="page">
      <header className="page-header">
        <h1>Proveedores</h1>
        <p>
          Catálogo de proveedores activos. Sin ingesta automática: la coordinación real con proveedores es por
          WhatsApp, sin ningún sistema del que importar esta información — el lead time se corrige a mano, a medida
          que se lo conoce mejor en la práctica.
        </p>
      </header>

      {loadError && (
        <p className="state state-error" role="alert">
          {loadError}
        </p>
      )}

      {!loadError && !suppliers && <p className="state">Cargando…</p>}

      {suppliers && suppliers.length === 0 && (
        <p className="state state-empty">No hay proveedores activos cargados.</p>
      )}

      {suppliers && suppliers.length > 0 && (
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Razón social</th>
                <th>Lead time (días)</th>
                <th>Condición de pago</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {suppliers.map((supplier) => (
                <tr key={supplier.supplierId}>
                  <td>{supplier.businessName}</td>
                  <td>
                    {editingId === supplier.supplierId ? (
                      <input
                        type="number"
                        min={1}
                        value={draftLeadTime}
                        onChange={(event) => setDraftLeadTime(event.target.value)}
                        autoFocus
                        className="lead-time-input"
                      />
                    ) : (
                      supplier.leadTimeDays
                    )}
                  </td>
                  <td>{supplier.paymentTerms}</td>
                  <td className="supplier-actions">
                    {editingId === supplier.supplierId ? (
                      <>
                        <button type="button" onClick={() => saveLeadTime(supplier.supplierId)} disabled={saving}>
                          {saving ? 'Guardando…' : 'Guardar'}
                        </button>
                        <button type="button" onClick={cancelEditing} disabled={saving}>
                          Cancelar
                        </button>
                      </>
                    ) : (
                      <button type="button" onClick={() => startEditing(supplier)}>
                        Editar lead time
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {rowError && (
        <p className="state state-error" role="alert">
          {rowError}
        </p>
      )}
    </main>
  )
}
