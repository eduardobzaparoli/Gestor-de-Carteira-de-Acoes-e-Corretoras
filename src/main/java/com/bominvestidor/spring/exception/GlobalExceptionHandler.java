package com.bominvestidor.spring.exception;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.bominvestidor.spring.dto.error.ApiErrorResponse;
import com.bominvestidor.spring.dto.error.FieldErrorResponse;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		List<FieldErrorResponse> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
				.sorted(Comparator.comparing(FieldErrorResponse::field))
				.toList();
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
				request.getRequestURI(), fieldErrors);
	}

	@ExceptionHandler(InvalidUserDataException.class)
	ResponseEntity<ApiErrorResponse> handleInvalidUserData(InvalidUserDataException exception,
			HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed",
				request.getRequestURI(), List.of(new FieldErrorResponse(exception.getField(), exception.getMessage())));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		return response(HttpStatus.BAD_REQUEST, "MALFORMED_JSON", "Request body is invalid",
				request.getRequestURI(), List.of());
	}

	@ExceptionHandler(DuplicateEmailException.class)
	ResponseEntity<ApiErrorResponse> handleDuplicateEmail(DuplicateEmailException exception,
			HttpServletRequest request) {
		return response(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", exception.getMessage(),
				request.getRequestURI(), List.of());
	}

	@ExceptionHandler({ InvalidCredentialsException.class, AuthenticatedUserNotFoundException.class })
	ResponseEntity<ApiErrorResponse> handleUnauthorized(RuntimeException exception, HttpServletRequest request) {
		String code = exception instanceof InvalidCredentialsException ? "INVALID_CREDENTIALS" : "UNAUTHORIZED";
		return response(HttpStatus.UNAUTHORIZED, code, exception.getMessage(), request.getRequestURI(), List.of());
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
		LOGGER.error("Unexpected request failure", exception);
		return response(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred",
				request.getRequestURI(), List.of());
	}

	private ResponseEntity<ApiErrorResponse> response(HttpStatus status, String code, String message, String path,
			List<FieldErrorResponse> fieldErrors) {
		ApiErrorResponse body = new ApiErrorResponse(Instant.now(), status.value(), code, message, path, fieldErrors);
		return ResponseEntity.status(status).body(body);
	}
}
