package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Traduce errores de entrada (parámetros faltantes, mal tipados, o que
 * violan una invariante de dominio) a respuestas HTTP 400 consistentes.
 * No contiene lógica de negocio: solo mapea excepciones ya lanzadas por
 * Spring MVC (binding/validación) o por el propio dominio
 * (InvalidDomainDataException) a un formato de error uniforme.
 * Cualquier otra excepción no capturada acá cae al 500 por defecto de
 * Spring — deliberadamente, para no enmascarar errores inesperados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Contraparte de domain.exception.ErrorCode para una condición que no
	 * nace en el dominio sino acá: la request no pasó la bean validation.
	 * Vive en el adaptador justamente por eso —el dominio no sabe que
	 * existe HTTP— pero comparte el mismo contrato de la propiedad `code`,
	 * así que el espejo del frontend la incluye junto a las demás.
	 */
	static final String VALIDATION_FAILED_CODE = "VALIDATION_FAILED";

	/**
	 * `code` solo viaja cuando la validación lo define (las que un usuario
	 * puede disparar desde un formulario, ver ErrorCode). Sin código, el
	 * cliente cae al `detail` en inglés, que es lo correcto para una
	 * invariante interna: no debería haber llegado una petición así.
	 */
	@ExceptionHandler(InvalidDomainDataException.class)
	public ProblemDetail handleInvalidDomainData(InvalidDomainDataException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
		if (ex.getCode() != null) {
			problem.setProperty("code", ex.getCode().name());
		}
		return problem;
	}

	/**
	 * `detail` queda como dato técnico (el mensaje de la excepción, en
	 * inglés); `code` y `resourceId` son el contrato legible por máquina
	 * para que el cliente redacte el mensaje en su idioma sin parsear ese
	 * texto — ver ErrorCode y, del lado del frontend, shared/apiError.ts.
	 */
	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail handleNotFound(NotFoundException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
		problem.setProperty("code", ex.getCode().name());
		problem.setProperty("resourceId", ex.getResourceId());
		return problem;
	}

	@ExceptionHandler(CsvIngestionThresholdExceededException.class)
	public ProblemDetail handleCsvIngestionThresholdExceeded(CsvIngestionThresholdExceededException ex) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
		problem.setProperty("totalRowsRead", ex.getTotalRowsRead());
		problem.setProperty("rejectedCount", ex.getRejectedCount());
		problem.setProperty("rejectionRatePercent", ex.getRejectionRatePercent());
		problem.setProperty("rejections", ex.getRejections());
		return problem;
	}

	/**
	 * Violación de bean validation sobre parámetros sueltos (@RequestParam
	 * o @PathVariable con @Positive y compañía). El mensaje que arma
	 * Jakarta —"getForecast.horizonDays: must be greater than 0"— nombra el
	 * método del controller y está en inglés, así que no sirve para
	 * mostrar: se publica el código y la lista de campos, y el cliente
	 * redacta (ver shared/apiError.ts).
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		List<String> invalidFields = ex.getConstraintViolations().stream()
				.map(violation -> lastNodeOf(violation.getPropertyPath().toString()))
				.distinct()
				.sorted()
				.toList();
		return validationProblem(invalidFields, ex.getMessage());
	}

	/**
	 * Violación de bean validation sobre un @RequestBody @Valid. Sin este
	 * handler, Spring resolvía la excepción por su cuenta y respondía 400
	 * con el cuerpo vacío: el cliente no recibía nada que mostrar.
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ProblemDetail handleRequestBodyValidation(MethodArgumentNotValidException ex) {
		List<String> invalidFields = ex.getBindingResult().getFieldErrors().stream()
				.map(FieldError::getField)
				.distinct()
				.sorted()
				.toList();
		return validationProblem(invalidFields, "Request body validation failed");
	}

	/**
	 * `detail` queda como dato técnico y `invalidFields` nombra los campos
	 * con el nombre que usa la API — útil para un log o para depurar otro
	 * cliente, no para una pantalla: el frontend muestra un texto genérico
	 * en español a partir del código.
	 */
	private static ProblemDetail validationProblem(List<String> invalidFields, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setProperty("code", VALIDATION_FAILED_CODE);
		problem.setProperty("invalidFields", invalidFields);
		return problem;
	}

	/** "getForecast.horizonDays" -> "horizonDays". */
	private static String lastNodeOf(String propertyPath) {
		int lastDot = propertyPath.lastIndexOf('.');
		return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
				"Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
	}
}
