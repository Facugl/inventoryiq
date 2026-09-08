import { toast, type ToastOptions } from 'react-toastify'

const DEFAULT_OPTIONS: ToastOptions = {
  position: 'bottom-right',
  autoClose: 3000,
  hideProgressBar: true,
  closeOnClick: true,
  pauseOnHover: true,
}

export function showSuccess(message: string, options: ToastOptions = {}) {
  toast.success(message, { ...DEFAULT_OPTIONS, ...options })
}

export function showError(message: string, options: ToastOptions = {}) {
  toast.error(message, { ...DEFAULT_OPTIONS, autoClose: 5000, ...options })
}
