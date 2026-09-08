import * as yup from 'yup'

export const categoryParametersSchema = yup.object({
  maxCoverageDaysThreshold: yup
    .number()
    .transform((value, original) => (original === '' ? NaN : value))
    .typeError('El umbral de sobrestock tiene que ser un número entero mayor a 0.')
    .integer('El umbral de sobrestock tiene que ser un número entero mayor a 0.')
    .positive('El umbral de sobrestock tiene que ser un número entero mayor a 0.')
    .required('El umbral de sobrestock tiene que ser un número entero mayor a 0.'),
  defaultExtraCoverageDays: yup
    .number()
    .transform((value, original) => (original === '' ? NaN : value))
    .typeError('El stock de seguridad extra tiene que ser un número entero, 0 o más.')
    .integer('El stock de seguridad extra tiene que ser un número entero, 0 o más.')
    .min(0, 'El stock de seguridad extra tiene que ser un número entero, 0 o más.')
    .required('El stock de seguridad extra tiene que ser un número entero, 0 o más.'),
})

export type CategoryParametersFormValues = yup.InferType<typeof categoryParametersSchema>
