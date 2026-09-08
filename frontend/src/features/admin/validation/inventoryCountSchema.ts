import * as yup from 'yup'

export const inventoryCountSchema = yup.object({
  quantity: yup
    .number()
    .transform((value, original) => (original === '' ? NaN : value))
    .typeError('Ingresá una cantidad válida (0 o más).')
    .integer('Ingresá una cantidad válida (0 o más).')
    .min(0, 'Ingresá una cantidad válida (0 o más).')
    .required('Ingresá una cantidad válida (0 o más).'),
})

export type InventoryCountFormValues = yup.InferType<typeof inventoryCountSchema>
