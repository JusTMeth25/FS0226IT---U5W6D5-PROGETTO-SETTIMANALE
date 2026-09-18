package com.example.demo.web;

import com.example.demo.dto.ApiError;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.ConflictException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.ServiceUnavailableException;
import com.example.demo.exception.UpstreamException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
		return build(HttpStatus.NOT_FOUND, exception.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	public ResponseEntity<ApiError> handleConflict(ConflictException exception) {
		return build(HttpStatus.CONFLICT, exception.getMessage());
	}

	@ExceptionHandler(BadRequestException.class)
	public ResponseEntity<ApiError> handleBadRequest(BadRequestException exception) {
		return build(HttpStatus.BAD_REQUEST, exception.getMessage());
	}

	@ExceptionHandler(ServiceUnavailableException.class)
	public ResponseEntity<ApiError> handleUnavailable(ServiceUnavailableException exception) {
		return build(HttpStatus.SERVICE_UNAVAILABLE, exception.getMessage());
	}

	/** The cause stays in the log; the client only gets the readable message. */
	@ExceptionHandler(UpstreamException.class)
	public ResponseEntity<ApiError> handleUpstream(UpstreamException exception) {
		if (exception.getCause() != null) {
			log.warn("Upstream failure: {}", exception.getCause().toString());
		}
		return build(HttpStatus.BAD_GATEWAY, exception.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.reduce((a, b) -> a + "; " + b)
				.orElse("Richiesta non valida");

		return build(HttpStatus.BAD_REQUEST, message);
	}

	/**
	 * Anything else. Spring's own web exceptions (404 on an unknown path, 405, missing
	 * parameters) keep their status; the rest is a bug: the stack trace goes to the log
	 * and the client gets a generic 500.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception exception) {
		if (exception instanceof ErrorResponse errorResponse) {
			HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
			if (status != null) {
				return build(status, exception.getMessage());
			}
		}
		log.error("Unhandled exception", exception);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of(500, "Internal Server Error", "Errore interno del server"));
	}

	private ResponseEntity<ApiError> build(HttpStatus status, String message) {
		log.warn("{} {}: {}", status.value(), status.getReasonPhrase(), message);
		return ResponseEntity.status(status)
				.body(ApiError.of(status.value(), status.getReasonPhrase(), message));
	}
}
