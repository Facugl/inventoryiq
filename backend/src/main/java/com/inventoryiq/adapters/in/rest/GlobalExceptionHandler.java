package com.inventoryiq.adapters.in.rest;

import com.inventoryiq.domain.exception.CsvIngestionThresholdExceededException;
import com.inventoryiq.domain.exception.InvalidDomainDataException;
import com.inventoryiq.domain.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

	@ExceptionHandler(InvalidDomainDataException.class)
	public ProblemDetail handleInvalidDomainData(InvalidDomainDataException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(NotFoundException.class)
	public ProblemDetail handleNotFound(NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
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

	@ExceptionHandler(ConstraintViolationException.class)
	public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
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
