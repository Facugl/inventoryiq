import * as yup from 'yup'

const REQUIRED_MESSAGE = 'Ingresá un número entero mayor a 0.'

export const leadTimeSchema = yup.object({
  leadTimeDays: yup
    .number()
    .transform((value, original) => (original === '' ? NaN : value))
    .typeError(REQUIRED_MESSAGE)
    .integer(REQUIRED_MESSAGE)
    .positive(REQUIRED_MESSAGE)
    .required(REQUIRED_MESSAGE),
})

export type LeadTimeFormValues = yup.InferType<typeof leadTimeSchema>
